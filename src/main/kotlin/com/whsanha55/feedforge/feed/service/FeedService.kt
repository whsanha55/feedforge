package com.whsanha55.feedforge.feed.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.whsanha55.feedforge.feed.dto.FeedItemPageResponse
import com.whsanha55.feedforge.feed.dto.FeedItemResponse
import com.whsanha55.feedforge.feed.enums.FeedSortEnum
import com.whsanha55.feedforge.feed.enums.toSort
import com.whsanha55.feedforge.feed.repository.FeedItemRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class FeedService(private val feedItemRepository: FeedItemRepository, private val objectMapper: ObjectMapper) {

    fun getFeedItems(sort: FeedSortEnum, page: Int, size: Int): FeedItemPageResponse {
        val pageable = PageRequest.of(page, size, sort.toSort())
        val result = feedItemRepository.findAllWithRawItem(pageable)
        val items = result.content.map { feedItem ->
            FeedItemResponse.from(feedItem, parseJson(feedItem.rawItem.extra), parseJson(feedItem.matchedTerms))
        }
        return FeedItemPageResponse(
            items = items,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
        )
    }

    private fun parseJson(json: String?): Map<*, *> = if (json == null) {
        emptyMap<Any, Any>()
    } else {
        runCatching { objectMapper.readValue(json, Map::class.java) }.getOrElse { emptyMap<Any, Any>() }
    }
}
