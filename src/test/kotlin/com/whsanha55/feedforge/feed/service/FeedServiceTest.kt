package com.whsanha55.feedforge.feed.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.whsanha55.feedforge.collect.entity.RawItemEntity
import com.whsanha55.feedforge.collect.enums.SourceEnum
import com.whsanha55.feedforge.collect.repository.RawItemRepository
import com.whsanha55.feedforge.feed.entity.FeedItemEntity
import com.whsanha55.feedforge.feed.enums.FeedSortEnum
import com.whsanha55.feedforge.feed.repository.FeedItemRepository
import com.whsanha55.feedforge.global.config.JpaConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(FeedService::class, JpaConfig::class, FeedServiceTest.TestConfig::class)
class FeedServiceTest @Autowired constructor(
    private val feedService: FeedService,
    private val rawItemRepository: RawItemRepository,
    private val feedItemRepository: FeedItemRepository,
) {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun clock(): Clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC)

        @Bean
        fun objectMapper(): ObjectMapper = ObjectMapper()
    }

    @Test
    fun `추천순 정렬로 점수 내림차순으로 조회한다`() {
        // given
        saveFeedItem(sourceKey = "1", title = "낮은 점수 딜", score = 1.0, itemAt = FIXED_NOW)
        saveFeedItem(sourceKey = "2", title = "높은 점수 딜", score = 4.2, itemAt = FIXED_NOW.minusSeconds(60))
        saveFeedItem(sourceKey = "3", title = "중간 점수 딜", score = 3.5, itemAt = FIXED_NOW.minusSeconds(120))

        // when
        val result = feedService.getFeedItems(FeedSortEnum.RECOMMENDED, page = 0, size = 10)

        // then
        assertThat(result.totalElements).isEqualTo(3)
        assertThat(result.items.map { it.title }).containsExactly("높은 점수 딜", "중간 점수 딜", "낮은 점수 딜")
    }

    @Test
    fun `최신순 정렬로 게시 시각 내림차순으로 조회한다`() {
        // given
        saveFeedItem(sourceKey = "4", title = "오래된 딜", score = 4.0, itemAt = FIXED_NOW.minusSeconds(3600))
        saveFeedItem(sourceKey = "5", title = "최신 딜", score = 1.0, itemAt = FIXED_NOW)

        // when
        val result = feedService.getFeedItems(FeedSortEnum.LATEST, page = 0, size = 10)

        // then
        assertThat(result.items.map { it.title }).containsExactly("최신 딜", "오래된 딜")
    }

    @Test
    fun `페이지네이션과 추천 근거를 함께 반환한다`() {
        // given
        saveFeedItem(sourceKey = "6", title = "첫 페이지 딜", score = 4.0, itemAt = FIXED_NOW)
        saveFeedItem(sourceKey = "7", title = "두번째 딜", score = 3.0, itemAt = FIXED_NOW.minusSeconds(60))

        // when
        val result = feedService.getFeedItems(FeedSortEnum.RECOMMENDED, page = 0, size = 1)

        // then
        assertThat(result.page).isZero()
        assertThat(result.size).isEqualTo(1)
        assertThat(result.totalElements).isEqualTo(2)
        assertThat(result.items).hasSize(1)

        val item = result.items.single()
        assertThat(item.title).isEqualTo("첫 페이지 딜")
        assertThat(item.source).isEqualTo("ZOD")
        assertThat(item.mall).isEqualTo("지마켓")
        assertThat(item.price).isEqualTo(543720L)
        assertThat(item.reason).isEqualTo("관심 매칭(SSD)")
        assertThat(item.matchedTerms).hasSize(1)
        assertThat(item.matchedTerms.single().value).isEqualTo("SSD")
    }

    // 수집 근거 데이터를 직접 심어 스코어링 없이 조회를 검증한다.
    private fun saveFeedItem(sourceKey: String, title: String, score: Double, itemAt: Instant) {
        val rawItem = rawItemRepository.save(
            RawItemEntity(
                source = SourceEnum.ZOD,
                provider = "zod",
                sourceKey = sourceKey,
                category = null,
                title = title,
                url = "https://zod.kr/deal/$sourceKey",
                canonicalUrl = null,
                extra = """{"mall":"지마켓","price":543720,"shippingFee":"무료","votes":1}""",
                itemAt = itemAt,
                firstSeenAt = FIXED_NOW,
                lastSeenAt = FIXED_NOW,
            ),
        )
        feedItemRepository.save(
            FeedItemEntity(
                rawItem = rawItem,
                score = score,
                matchedTerms = MATCHED_TERMS_JSON,
                popular = false,
                scoringVersion = "v1",
                recommendedAt = FIXED_NOW,
                evaluatedAt = FIXED_NOW,
                expiresAt = itemAt.plusSeconds(FRESHNESS_SECONDS),
            ),
        )
    }

    companion object {
        private val FIXED_NOW: Instant = Instant.parse("2026-10-13T06:00:00Z")

        private const val FRESHNESS_SECONDS = 72 * 3600L

        private const val MATCHED_TERMS_JSON =
            """{"reason":"관심 매칭(SSD)","terms":[{"kind":"KEYWORD","value":"SSD","weight":2.0}],"popularityBonus":1.0}"""

        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:16"))
    }
}
