package com.whsanha55.feedforge.collect.repository

import com.whsanha55.feedforge.collect.entity.RawItemEntity
import com.whsanha55.feedforge.collect.enums.SourceEnum
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface RawItemRepository : JpaRepository<RawItemEntity, Long> {
    fun findBySourceAndProviderAndSourceKey(source: SourceEnum, provider: String, sourceKey: String): RawItemEntity?

    fun findByItemAtAfter(itemAt: Instant): List<RawItemEntity>
}
