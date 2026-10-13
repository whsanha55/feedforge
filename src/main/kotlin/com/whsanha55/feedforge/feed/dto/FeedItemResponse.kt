package com.whsanha55.feedforge.feed.dto

import com.whsanha55.feedforge.feed.entity.FeedItemEntity
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class FeedItemResponse(
    @Schema(description = "피드 항목 ID", example = "1")
    val id: Long,
    @Schema(description = "수집 소스", example = "ZOD")
    val source: String,
    @Schema(description = "제목", example = "WD SN850X 2TB")
    val title: String,
    @Schema(description = "원문 링크", example = "https://zod.kr/deal/8887617")
    val url: String,
    @Schema(description = "판매처", example = "지마켓")
    val mall: String?,
    @Schema(description = "가격(원)", example = "149000")
    val price: Long?,
    @Schema(description = "배송비", example = "무료")
    val shippingFee: String?,
    @Schema(description = "원본 게시 시각")
    val itemAt: Instant,
    @Schema(description = "추천 시각")
    val recommendedAt: Instant,
    @Schema(description = "추천 점수", example = "3.5")
    val score: Double,
    @Schema(description = "콜드스타트 인기 노출 여부")
    val popular: Boolean,
    @Schema(description = "추천 이유", example = "관심 매칭(SSD)")
    val reason: String,
    @Schema(description = "매칭된 관심 항목")
    val matchedTerms: List<MatchedTermResponse>,
) {
    data class MatchedTermResponse(
        @Schema(description = "프로필 종류", example = "KEYWORD")
        val kind: String,
        @Schema(description = "값", example = "SSD")
        val value: String,
        @Schema(description = "당시 가중치", example = "2.0")
        val weight: Double,
    )

    companion object {
        fun from(feedItem: FeedItemEntity, extra: Map<*, *>, matchedTerms: Map<*, *>): FeedItemResponse {
            val rawItem = feedItem.rawItem
            return FeedItemResponse(
                id = requireNotNull(feedItem.id),
                source = rawItem.source.name,
                title = rawItem.title,
                url = rawItem.url,
                mall = extra[MALL_KEY] as? String,
                price = (extra[PRICE_KEY] as? Number)?.toLong(),
                shippingFee = extra[SHIPPING_KEY] as? String,
                itemAt = rawItem.itemAt,
                recommendedAt = feedItem.recommendedAt,
                score = feedItem.score,
                popular = feedItem.popular,
                reason = (matchedTerms[REASON_KEY] as? String).orEmpty(),
                matchedTerms = (matchedTerms[TERMS_KEY] as? List<*>).orEmpty().mapNotNull { term ->
                    val termMap = term as? Map<*, *> ?: return@mapNotNull null
                    MatchedTermResponse(
                        kind = termMap[KIND_KEY] as? String ?: return@mapNotNull null,
                        value = termMap[VALUE_KEY] as? String ?: return@mapNotNull null,
                        weight = (termMap[WEIGHT_KEY] as? Number)?.toDouble() ?: 0.0,
                    )
                },
            )
        }

        private const val MALL_KEY = "mall"
        private const val PRICE_KEY = "price"
        private const val SHIPPING_KEY = "shippingFee"
        private const val REASON_KEY = "reason"
        private const val TERMS_KEY = "terms"
        private const val KIND_KEY = "kind"
        private const val VALUE_KEY = "value"
        private const val WEIGHT_KEY = "weight"
    }
}
