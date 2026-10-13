package com.whsanha55.feedforge.profile.repository

import com.whsanha55.feedforge.profile.entity.InterestProfileEntity
import org.springframework.data.jpa.repository.JpaRepository

// 단일 사용자 프로필은 수십 건 수준이라 전체 조회로 읽는다.
interface InterestProfileRepository : JpaRepository<InterestProfileEntity, Long>
