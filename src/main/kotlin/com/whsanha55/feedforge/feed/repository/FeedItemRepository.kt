package com.whsanha55.feedforge.feed.repository

import com.whsanha55.feedforge.feed.entity.FeedItemEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FeedItemRepository : JpaRepository<FeedItemEntity, Long> {

    @Query(
        """
        SELECT f
        FROM FeedItemEntity f
        WHERE f.rawItem.id IN :rawItemIds
        """,
    )
    fun findAllByRawItemIds(rawItemIds: List<Long>): List<FeedItemEntity>

    @Query(
        """
        SELECT f
        FROM FeedItemEntity f
        JOIN FETCH f.rawItem
        """,
    )
    fun findAllWithRawItem(pageable: Pageable): Page<FeedItemEntity>
}
