package com.whsanha55.feedforge.scoring

import com.fasterxml.jackson.databind.ObjectMapper
import com.whsanha55.feedforge.collect.entity.RawItemEntity
import com.whsanha55.feedforge.collect.repository.RawItemRepository
import com.whsanha55.feedforge.profile.entity.InterestProfileEntity
import com.whsanha55.feedforge.profile.enums.ProfileKindEnum
import com.whsanha55.feedforge.profile.repository.InterestProfileRepository
import com.whsanha55.feedforge.scoring.entity.FeedItemEntity
import com.whsanha55.feedforge.scoring.repository.FeedItemRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import kotlin.math.min

private val log = KotlinLogging.logger {}

@Service
class ScoringService(
    private val rawItemRepository: RawItemRepository,
    private val interestProfileRepository: InterestProfileRepository,
    private val feedItemRepository: FeedItemRepository,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {

    private class TermContribution(val kind: ProfileKindEnum, val value: String, val weight: Double)

    private class ScoreResult(val terms: List<TermContribution>, val popularityBonus: Double, val score: Double)

    // 신선도 기간 내 원본을 현재 프로필로 다시 평가한다 (PLAN 8.5).
    @Transactional
    fun evaluateRecentItems() {
        val now = Instant.now(clock)
        val profiles = interestProfileRepository.findAll()
        val candidates = rawItemRepository.findByItemAtAfter(now.minus(ScoringPolicy.FRESHNESS))
        val existingByRawId = feedItemRepository
            .findAllByRawItemIds(candidates.mapNotNull { it.id })
            .associateByTo(mutableMapOf()) { it.rawItem.id }

        var recommended = 0
        for (rawItem in candidates) {
            val result = score(rawItem, profiles) ?: continue
            existingByRawId[rawItem.id] = upsert(rawItem, result, existingByRawId[rawItem.id], popular = false, now)
            recommended++
        }

        val popularCount = evaluatePopularItems(candidates, existingByRawId, profiles, now)
        log.info {
            "스코어링 완료. candidates=${candidates.size}, recommended=$recommended, popular=$popularCount"
        }
    }

    // 콜드스타트: 관심 항목이 부족한 동안 인기 상위를 함께 노출한다 (PLAN 8.3).
    private fun evaluatePopularItems(
        candidates: List<RawItemEntity>,
        existingByRawId: MutableMap<Long?, FeedItemEntity>,
        profiles: List<InterestProfileEntity>,
        now: Instant,
    ): Int {
        val interestCount = profiles.count { it.kind != ProfileKindEnum.SOURCE }
        if (interestCount >= ScoringPolicy.COLDSTART_PROFILE_MIN) {
            return 0
        }
        val topPopular = candidates
            .sortedByDescending(::votesOf)
            .take(ScoringPolicy.POPULAR_FEED_SIZE)

        var created = 0
        for (rawItem in topPopular) {
            val existing = existingByRawId[rawItem.id]
            // 이미 키워드 추천된 항목은 그 이유를 유지한다.
            if (existing != null && !existing.popular) {
                continue
            }
            val bonus = popularityBonus(rawItem)
            val result = ScoreResult(
                terms = emptyList(),
                popularityBonus = bonus,
                score = bonus,
            )
            existingByRawId[rawItem.id] = upsert(rawItem, result, existing, popular = true, now)
            created++
        }
        return created
    }

    // 관심 키워드·카테고리가 하나 이상 매칭되고 총점이 임계치 이상이어야 추천한다 (PLAN 8.1).
    private fun score(rawItem: RawItemEntity, profiles: List<InterestProfileEntity>): ScoreResult? {
        val matchedKeywords = profiles
            .filter { it.kind == ProfileKindEnum.KEYWORD && KeywordMatcher.matches(rawItem.title, it.value) }
            .map { TermContribution(it.kind, it.value, effectiveWeight(it)) }
        val matchedCategories = profiles
            .filter {
                it.kind == ProfileKindEnum.CATEGORY &&
                    rawItem.category != null &&
                    it.value.equals(rawItem.category, ignoreCase = true)
            }
            .map { TermContribution(it.kind, it.value, effectiveWeight(it)) }
        val sourceWeight = profiles
            .filter { it.kind == ProfileKindEnum.SOURCE && it.value == rawItem.source.name }
            .sumOf { effectiveWeight(it) }

        val matchedTerms = matchedKeywords + matchedCategories
        val popularityBonus = popularityBonus(rawItem)
        val total = matchedTerms.sumOf { it.weight } + sourceWeight + popularityBonus
        return when {
            matchedTerms.isEmpty() -> null
            total < ScoringPolicy.WEB_THRESHOLD -> null
            else -> ScoreResult(matchedTerms, popularityBonus, total)
        }
    }

    private fun upsert(
        rawItem: RawItemEntity,
        result: ScoreResult,
        existing: FeedItemEntity?,
        popular: Boolean,
        now: Instant,
    ): FeedItemEntity {
        val matchedTerms = objectMapper.writeValueAsString(
            mapOf(
                "reason" to reasonOf(result, popular),
                "terms" to result.terms.map {
                    mapOf("kind" to it.kind.name, "value" to it.value, "weight" to it.weight)
                },
                "popularityBonus" to result.popularityBonus,
            ),
        )
        if (existing == null) {
            return feedItemRepository.save(
                FeedItemEntity(
                    rawItem = rawItem,
                    score = result.score,
                    matchedTerms = matchedTerms,
                    popular = popular,
                    scoringVersion = ScoringPolicy.SCORING_VERSION,
                    recommendedAt = now,
                    evaluatedAt = now,
                    expiresAt = rawItem.itemAt.plus(ScoringPolicy.FRESHNESS),
                ),
            )
        }
        existing.updateEvaluation(result.score, matchedTerms, now)
        return existing
    }

    private fun reasonOf(result: ScoreResult, popular: Boolean): String {
        if (popular) {
            return "인기 항목"
        }
        return "관심 매칭(" + result.terms.joinToString(", ") { it.value } + ")"
    }

    private fun effectiveWeight(profile: InterestProfileEntity): Double = profile.seedWeight + profile.learnedWeight

    private fun popularityBonus(rawItem: RawItemEntity): Double =
        min(votesOf(rawItem) * ScoringPolicy.POPULARITY_COEFFICIENT, ScoringPolicy.POPULARITY_CAP)

    private fun votesOf(rawItem: RawItemEntity): Long =
        (parseExtra(rawItem.extra)[VOTES_KEY] as? Number)?.toLong() ?: 0L

    private fun parseExtra(extra: String?): Map<*, *> = if (extra == null) {
        emptyMap<Any, Any>()
    } else {
        runCatching { objectMapper.readValue(extra, Map::class.java) }.getOrElse { emptyMap<Any, Any>() }
    }

    companion object {
        private const val VOTES_KEY = "votes"
    }
}
