package com.whsanha55.feedforge.scoring.entity

import com.whsanha55.feedforge.collect.entity.RawItemEntity
import com.whsanha55.feedforge.global.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

// 수집 원본 추천 근거는 보존할 필드가 많아 생성자 파라미터 수가 제한보다 많다.
@Suppress("LongParameterList")
@Entity
@Table(name = "feed_item")
class FeedItemEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_item_id", nullable = false, unique = true)
    val rawItem: RawItemEntity,

    score: Double,

    // 매칭 항목·가중치·기여도와 추천 이유. 직렬화는 서비스 계층에서 담당한다.
    matchedTerms: String?,

    // 콜드스타트 인기 노출 여부. 추천 이유 표기와 재평가 대상 판단에 쓴다.
    @Column(nullable = false)
    val popular: Boolean,

    @Column(nullable = false, length = 16)
    val scoringVersion: String,

    @Column(nullable = false)
    val recommendedAt: Instant,

    evaluatedAt: Instant,

    @Column
    val expiresAt: Instant?,
) : BaseEntity() {

    @Column(nullable = false)
    var score: Double = score
        protected set

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var matchedTerms: String? = matchedTerms
        protected set

    @Column(nullable = false)
    var evaluatedAt: Instant = evaluatedAt
        protected set

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    // 재평가 시 점수와 근거만 갱신한다. 최초 추천 시각은 유지한다.
    fun updateEvaluation(score: Double, matchedTerms: String?, evaluatedAt: Instant) {
        this.score = score
        this.matchedTerms = matchedTerms
        this.evaluatedAt = evaluatedAt
    }
}
