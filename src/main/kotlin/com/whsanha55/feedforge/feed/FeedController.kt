package com.whsanha55.feedforge.feed

import com.whsanha55.feedforge.feed.dto.FeedItemPageResponse
import com.whsanha55.feedforge.feed.enums.FeedSortEnum
import com.whsanha55.feedforge.feed.service.FeedService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "feed", description = "추천 피드")
@RestController
class FeedController(private val feedService: FeedService) {

    @Operation(summary = "추천 피드 조회")
    @GetMapping("/feedforge/feed-items")
    fun getFeedItems(
        @Parameter(description = "정렬 기준")
        @RequestParam(defaultValue = "RECOMMENDED")
        sort: FeedSortEnum,
        @Parameter(description = "페이지 번호(0부터)")
        @RequestParam(defaultValue = "0")
        page: Int,
        @Parameter(description = "페이지 크기")
        @RequestParam(defaultValue = "20")
        size: Int,
    ): FeedItemPageResponse = feedService.getFeedItems(sort, page, size)
}
