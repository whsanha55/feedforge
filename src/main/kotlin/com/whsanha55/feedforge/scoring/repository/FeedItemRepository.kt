package com.whsanha55.feedforge.scoring.repository

import com.whsanha55.feedforge.scoring.entity.FeedItemEntity
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
}
