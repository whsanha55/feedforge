package com.whsanha55.feedforge.collect.source.zod

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// zod.kr/deal 목록 페이지 파서. HTML fixture 기반 회귀 테스트로 보호한다.
object ZodParser {
    private const val DEAL_BASE_URL = "https://zod.kr/deal/"

    // 게시판의 절대 시각은 사이트 기준 시간대(KST)로 표기된다.
    private val ZOD_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    private val ITEM_AT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private const val MALL_KEY = "홈페이지/장소"
    private const val PRICE_KEY = "가격"
    private const val SHIPPING_KEY = "배송비"

    fun parse(html: String): List<ZodDeal> = Jsoup.parse(html)
        .select("a.app-list-link")
        .mapNotNull(::toDeal)

    private class DealCore(val sourceKey: String, val title: String, val itemAt: Instant)

    private fun toDeal(link: Element): ZodDeal? {
        val core = extractCore(link) ?: return null
        val dealMeta = link.selectFirst("dl.zod-board--deal-meta")
            ?.select("dt")
            ?.associateBy({ dt -> dt.text().trim() }, { dt -> dt.nextElementSibling() })
            .orEmpty()
        val votes = link.selectFirst(".app-list__voted-count")?.text()
            ?.filter(Char::isDigit)
            ?.takeIf { it.isNotEmpty() }
            ?.toLong()
            ?: 0L

        return ZodDeal(
            sourceKey = core.sourceKey,
            title = core.title,
            url = DEAL_BASE_URL + core.sourceKey,
            mall = dealMeta[MALL_KEY]?.selectFirst("strong")?.text()?.trim()?.takeIf { it.isNotEmpty() },
            price = dealMeta[PRICE_KEY]?.let(::parsePrice),
            shippingFee = dealMeta[SHIPPING_KEY]?.selectFirst("strong")?.text()?.trim(),
            itemAt = core.itemAt,
            votes = votes,
        )
    }

    // 식별에 필수인 필드가 하나라도 없으면 이 항목은 건너뛴다.
    private fun extractCore(link: Element): DealCore? {
        val sourceKey = link.attr("href").substringAfterLast("/").takeIf { it.isNotEmpty() }
        val title = link.selectFirst(".app-list-title-item")?.text()?.trim()?.takeIf { it.isNotEmpty() }
        val itemAt = link.selectFirst("dl.app-list-meta dd[title]")?.attr("title")?.let(::parseItemAt)
        if (sourceKey == null || title == null || itemAt == null) {
            return null
        }
        return DealCore(sourceKey, title, itemAt)
    }

    private fun parseItemAt(value: String): Instant? = runCatching {
        LocalDateTime.parse(value, ITEM_AT_FORMAT).atZone(ZOD_ZONE).toInstant()
    }.getOrNull()

    private fun parsePrice(element: Element): Long? {
        val text = element.selectFirst("strong")?.text() ?: element.text()
        val digits = text.filter(Char::isDigit)
        if (digits.isEmpty()) {
            return null
        }
        return digits.toLong()
    }
}
