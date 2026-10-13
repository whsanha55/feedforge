package com.whsanha55.feedforge.web.dto

import io.swagger.v3.oas.annotations.media.Schema

data class MeResponse(
    @field:Schema(description = "사용자 식별자", example = "owner")
    val user: String,
)
