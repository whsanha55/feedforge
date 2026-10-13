package com.whsanha55.feedforge.profile.entity

import com.whsanha55.feedforge.global.base.BaseEntity
import com.whsanha55.feedforge.profile.enums.ProfileKindEnum
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

// 유효 가중치는 seed_weight + learned_weight로 계산한다.
@Entity
@Table(name = "interest_profile")
class InterestProfileEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val kind: ProfileKindEnum,

    @Column(nullable = false, length = 128)
    val value: String,

    @Column(nullable = false)
    val seedWeight: Double,

    @Column(nullable = false)
    val learnedWeight: Double,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
