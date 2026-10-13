package com.whsanha55.feedforge.collect.entity

import com.whsanha55.feedforge.collect.enums.AvailabilityEnum
import com.whsanha55.feedforge.collect.enums.SourceEnum
import com.whsanha55.feedforge.global.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

// 수집 원본은 보존할 컬럼이 많아 생성자 파라미터 수가 제한보다 많다.
@Suppress("LongParameterList")
@Entity
@Table(name = "raw_item")
class RawItemEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val source: SourceEnum,

    @Column(nullable = false, length = 32)
    val provider: String,

    @Column(nullable = false, length = 128)
    val sourceKey: String,

    @Column(length = 64)
    val category: String?,

    @Column(nullable = false, length = 1024)
    val title: String,

    @Column(nullable = false, length = 1024)
    val url: String,

    @Column(length = 1024)
    val canonicalUrl: String?,

    // JSONB 원문 메타데이터. 직렬화는 서비스 계층에서 담당한다.
    extra: String?,

    @Column(nullable = false)
    val itemAt: Instant,

    @Column(nullable = false)
    val firstSeenAt: Instant,

    lastSeenAt: Instant,
) : BaseEntity() {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var extra: String? = extra
        protected set

    @Column(nullable = false)
    var lastSeenAt: Instant = lastSeenAt
        protected set

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var availability: AvailabilityEnum = AvailabilityEnum.UNKNOWN
        protected set

    // 재수집 시 변경 가능한 메타데이터만 갱신한다.
    fun refresh(lastSeenAt: Instant, extra: String?) {
        this.lastSeenAt = lastSeenAt
        this.extra = extra
    }
}
