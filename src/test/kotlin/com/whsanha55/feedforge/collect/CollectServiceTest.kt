package com.whsanha55.feedforge.collect

import com.fasterxml.jackson.databind.ObjectMapper
import com.whsanha55.feedforge.collect.enums.SourceEnum
import com.whsanha55.feedforge.collect.repository.RawItemRepository
import com.whsanha55.feedforge.collect.source.zod.ZodDeal
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
@Import(CollectService::class, JpaConfig::class, CollectServiceTest.TestConfig::class)
class CollectServiceTest @Autowired constructor(
    private val collectService: CollectService,
    private val rawItemRepository: RawItemRepository,
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
    fun `신규 딜을 저장하면 원본 항목이 생성된다`() {
        // given
        val deal = deal(sourceKey = "100", votes = 3)

        // when
        collectService.saveZodDeals(listOf(deal))

        // then
        val saved = requireNotNull(
            rawItemRepository.findBySourceAndProviderAndSourceKey(SourceEnum.ZOD, "zod", "100"),
        )
        assertThat(saved.title).isEqualTo("테스트 딜")
        assertThat(saved.itemAt).isEqualTo(deal.itemAt)
        assertThat(saved.firstSeenAt).isEqualTo(FIXED_NOW)
        assertThat(saved.lastSeenAt).isEqualTo(FIXED_NOW)
        assertThat(saved.extra).contains("\"votes\":3")
    }

    @Test
    fun `같은 딜을 재수집하면 항목 하나로 유지되고 메타데이터가 갱신된다`() {
        // given
        collectService.saveZodDeals(listOf(deal(sourceKey = "100", votes = 1)))
        val later = FIXED_NOW.plusSeconds(1800)
        clock.advanceTo(later)

        // when
        collectService.saveZodDeals(listOf(deal(sourceKey = "100", votes = 7)))

        // then
        val items = rawItemRepository.findAll()
        assertThat(items).hasSize(1)
        val item = items.single()
        assertThat(item.firstSeenAt).isEqualTo(FIXED_NOW)
        assertThat(item.lastSeenAt).isEqualTo(later)
        assertThat(item.extra).contains("\"votes\":7")
    }

    private fun deal(sourceKey: String, votes: Long) = ZodDeal(
        sourceKey = sourceKey,
        title = "테스트 딜",
        url = "https://zod.kr/deal/$sourceKey",
        mall = "지마켓",
        price = 10000L,
        shippingFee = "무료",
        itemAt = FIXED_NOW.minusSeconds(600),
        votes = votes,
    )

    companion object {
        private val FIXED_NOW: Instant = Instant.parse("2026-10-13T06:00:00Z")

        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:16"))
    }
}
