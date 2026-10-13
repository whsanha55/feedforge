package com.whsanha55.feedforge.feed.dto

import io.swagger.v3.oas.annotations.media.Schema

data class FeedItemPageResponse(
    @Schema(description = "피드 항목 목록")
    val items: List<FeedItemResponse>,
    @Schema(description = "페이지 번호(0부터)", example = "0")
    val page: Int,
    @Schema(description = "페이지 크기", example = "20")
    val size: Int,
    @Schema(description = "전체 건수", example = "42")
    val totalElements: Long,
)
