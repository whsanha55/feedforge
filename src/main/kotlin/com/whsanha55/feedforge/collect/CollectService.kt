package com.whsanha55.feedforge.collect

import com.fasterxml.jackson.databind.ObjectMapper
import com.whsanha55.feedforge.collect.entity.RawItemEntity
import com.whsanha55.feedforge.collect.enums.SourceEnum
import com.whsanha55.feedforge.collect.repository.RawItemRepository
import com.whsanha55.feedforge.collect.source.zod.ZodDeal
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

private val log = KotlinLogging.logger {}

@Service
class CollectService(
    private val rawItemRepository: RawItemRepository,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {

    @Transactional
    fun saveZodDeals(deals: List<ZodDeal>) {
        val now = Instant.now(clock)
        var created = 0
        var refreshed = 0
        for (deal in deals) {
            val existing = rawItemRepository.findBySourceAndProviderAndSourceKey(
                SourceEnum.ZOD,
                ZOD_PROVIDER,
                deal.sourceKey,
            )
            if (existing == null) {
                rawItemRepository.save(deal.toEntity(now))
                created++
            } else {
                existing.refresh(lastSeenAt = now, extra = objectMapper.writeValueAsString(deal.toExtraMap()))
                refreshed++
            }
        }
        log.info { "zod 수집 저장 완료. created=$created, refreshed=$refreshed" }
    }

    private fun ZodDeal.toEntity(now: Instant) = RawItemEntity(
        source = SourceEnum.ZOD,
        provider = ZOD_PROVIDER,
        sourceKey = sourceKey,
        category = null,
        title = title,
        url = url,
        canonicalUrl = null,
        extra = objectMapper.writeValueAsString(toExtraMap()),
        itemAt = itemAt,
        firstSeenAt = now,
        lastSeenAt = now,
    )

    private fun ZodDeal.toExtraMap(): Map<String, Any?> = mapOf(
        "mall" to mall,
        "price" to price,
        "shippingFee" to shippingFee,
        "votes" to votes,
    )

    companion object {
        private const val ZOD_PROVIDER = "zod"
    }
}
