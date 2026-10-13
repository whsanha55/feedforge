package com.whsanha55.feedforge.scoring

import com.fasterxml.jackson.databind.ObjectMapper
import com.whsanha55.feedforge.collect.CollectService
import com.whsanha55.feedforge.collect.source.zod.ZodDeal
import com.whsanha55.feedforge.feed.repository.FeedItemRepository
import com.whsanha55.feedforge.global.config.JpaConfig
import com.whsanha55.feedforge.profile.entity.InterestProfileEntity
import com.whsanha55.feedforge.profile.enums.ProfileKindEnum
import com.whsanha55.feedforge.profile.repository.InterestProfileRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
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
import java.time.ZoneId
import java.time.ZoneOffset

// 수집 주기 사이 시간 흐름을 시뮬레이션하기 위한 가변 시계.
class MutableClock(start: Instant) : Clock() {
    private var current = start

    fun advanceTo(instant: Instant) {
        current = instant
    }

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = current
}

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(
    ScoringService::class,
    CollectService::class,
    JpaConfig::class,
    ScoringServiceTest.TestConfig::class,
)
class ScoringServiceTest @Autowired constructor(
    private val scoringService: ScoringService,
    private val collectService: CollectService,
    private val interestProfileRepository: InterestProfileRepository,
    private val feedItemRepository: FeedItemRepository,
    private val clock: MutableClock,
) {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun clock(): MutableClock = MutableClock(FIXED_NOW)

        @Bean
        fun objectMapper(): ObjectMapper = ObjectMapper()
    }

    @Test
    fun `매칭된 키워드 가중치 합이 임계치를 넘으면 추천 항목을 만든다`() {
        // given
        saveProfile(ProfileKindEnum.KEYWORD, "SSD", 2.0)
        saveProfile(ProfileKindEnum.KEYWORD, "래칫", 1.5)
        saveRawItem(sourceKey = "1", title = "WD SSD 2TB 특가", votes = 10)

        // when
        scoringService.evaluateRecentItems()

        // then
        val feedItems = feedItemRepository.findAll()
        assertThat(feedItems).hasSize(1)
        val feedItem = feedItems.single()
        assertThat(feedItem.popular).isFalse()
        // score = 키워드 2.0 + 인기 보너스(10 * 0.1)
        assertThat(feedItem.score).isCloseTo(3.0, Percentage.withPercentage(1.0))
        assertThat(feedItem.matchedTerms).contains("\"value\":\"SSD\"")
        assertThat(feedItem.matchedTerms).contains("관심 매칭(SSD)")
    }

    @Test
    fun `매칭된 키워드가 없으면 추천하지 않는다`() {
        // given
        saveEnoughInterests("SSD")
        saveRawItem(sourceKey = "2", title = "전기밥솥 특가", votes = 10)

        // when
        scoringService.evaluateRecentItems()

        // then
        assertThat(feedItemRepository.findAll()).isEmpty()
    }

    @Test
    fun `매칭이 있어도 총점이 임계치 미만이면 추천하지 않는다`() {
        // given
        saveProfile(ProfileKindEnum.KEYWORD, "SSD", 1.0)
        saveEnoughInterests("밥솥", "SSD")
        saveRawItem(sourceKey = "3", title = "SSD 특가", votes = 0)

        // when
        scoringService.evaluateRecentItems()

        // then
        assertThat(feedItemRepository.findAll()).isEmpty()
    }

    @Test
    fun `관심 매칭 없이 소스 가중치만으로는 일반 추천이 되지 않는다`() {
        // given
        saveProfile(ProfileKindEnum.SOURCE, "ZOD", 5.0)
        saveRawItem(sourceKey = "4", title = "아무렇게나 좋은 제품", votes = 100)

        // when
        scoringService.evaluateRecentItems()

        // then: 콜드스타트(관심 0개)라 인기 노출로만 생성된다
        val feedItem = feedItemRepository.findAll().single()
        assertThat(feedItem.popular).isTrue()
        assertThat(feedItem.matchedTerms).contains("인기 항목")
    }

    @Test
    fun `관심 항목이 부족하면 인기 상위를 함께 노출한다`() {
        // given
        saveProfile(ProfileKindEnum.KEYWORD, "SSD", 1.0)
        saveRawItem(sourceKey = "10", title = "SSD 특가", votes = 5)
        saveRawItem(sourceKey = "11", title = "무관심 제품 A", votes = 50)
        saveRawItem(sourceKey = "12", title = "무관심 제품 B", votes = 30)

        // when
        scoringService.evaluateRecentItems()

        // then: 관심 1개(< 5)라 세 항목 모두 인기 노출된다
        val feedItems = feedItemRepository.findAll()
        assertThat(feedItems).hasSize(3)
        val popularTitles = feedItems.filter { it.popular }.map { it.rawItem.title }
        assertThat(popularTitles).containsExactlyInAnyOrder("SSD 특가", "무관심 제품 A", "무관심 제품 B")
    }

    @Test
    fun `관심 항목이 충분하면 인기 노출을 만들지 않는다`() {
        // given
        saveEnoughInterests("SSD")
        saveRawItem(sourceKey = "20", title = "무관심 제품", votes = 100)

        // when
        scoringService.evaluateRecentItems()

        // then
        assertThat(feedItemRepository.findAll()).isEmpty()
    }

    @Test
    fun `재평가에서 원본 메타데이터가 바뀌면 기존 항목 점수를 갱신한다`() {
        // given
        saveProfile(ProfileKindEnum.KEYWORD, "SSD", 2.0)
        saveRawItem(sourceKey = "30", title = "SSD 특가", votes = 10)
        scoringService.evaluateRecentItems()
        val first = feedItemRepository.findAll().single()

        // when
        clock.advanceTo(FIXED_NOW.plusSeconds(1800))
        saveRawItem(sourceKey = "30", title = "SSD 특가", votes = 50)
        scoringService.evaluateRecentItems()

        // then: 인기 보너스 상한(2.0)까지 반영해 갱신한다
        val after = feedItemRepository.findAll().single()
        assertThat(after.id).isEqualTo(first.id)
        assertThat(after.score).isCloseTo(4.0, Percentage.withPercentage(1.0))
        assertThat(after.evaluatedAt).isEqualTo(FIXED_NOW.plusSeconds(1800))
        assertThat(after.recommendedAt).isEqualTo(first.recommendedAt)
    }

    private fun saveEnoughInterests(vararg excludeValues: String) {
        // 콜드스타트 인기 노출을 끄기 위해 관심 키워드 5개를 채운다.
        val fillers = listOf("에어프라이어", "캠핑", "커피머신", "안마의자", "프로젝터")
        fillers.filter { it !in excludeValues }.forEach { saveProfile(ProfileKindEnum.KEYWORD, it, 0.5) }
    }

    private fun saveProfile(kind: ProfileKindEnum, value: String, weight: Double) {
        interestProfileRepository.save(
            InterestProfileEntity(
                kind = kind,
                value = value,
                seedWeight = weight,
                learnedWeight = 0.0,
            ),
        )
    }

    private fun saveRawItem(sourceKey: String, title: String, votes: Long) {
        collectService.saveZodDeals(
            listOf(
                ZodDeal(
                    sourceKey = sourceKey,
                    title = title,
                    url = "https://zod.kr/deal/$sourceKey",
                    mall = "지마켓",
                    price = 10000L,
                    shippingFee = "무료",
                    itemAt = FIXED_NOW.minusSeconds(600),
                    votes = votes,
                ),
            ),
        )
    }

    companion object {
        private val FIXED_NOW: Instant = Instant.parse("2026-10-13T06:00:00Z")

        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:16"))
    }
}
