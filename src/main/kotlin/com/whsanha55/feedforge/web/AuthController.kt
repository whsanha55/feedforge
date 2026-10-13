package com.whsanha55.feedforge.web

import com.whsanha55.feedforge.web.dto.MeResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "auth", description = "인증 상태 확인")
@RestController
class AuthController {

    @Operation(summary = "인증된 사용자 확인")
    @GetMapping("/feedforge/me")
    fun me(): MeResponse = MeResponse(user = "owner")
}
