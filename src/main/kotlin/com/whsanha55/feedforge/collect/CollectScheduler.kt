package com.whsanha55.feedforge.collect

import com.whsanha55.feedforge.collect.source.zod.ZodCollector
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

// 소스별 실패를 겹리해 한 소스 오류가 다른 수집을 막지 않게 한다.
@Component
class CollectScheduler(
    private val zodCollector: ZodCollector,
    private val collectService: CollectService,
    private val properties: CollectProperties,
) {

    // 소스 실패 격리가 목적이라 모든 예외를 잡는다 (PLAN 4절 설계 원칙).
    @Suppress("TooGenericExceptionCaught")
    @Scheduled(initialDelayString = "PT10S", fixedDelayString = "PT30M")
    fun collectZod() {
        if (!properties.zod.enabled) {
            return
        }
        try {
            val deals = zodCollector.fetchDeals()
            if (deals.isEmpty()) {
                log.warn { "zod 수집 결과 0건. HTTP 200이지만 파싱 실패 가능. 파서 변경 여부 확인 필요" }
                return
            }
            collectService.saveZodDeals(deals)
        } catch (e: Exception) {
            log.warn(e) { "zod 수집 실패. 다음 주기에 재시도한다" }
        }
    }
}
