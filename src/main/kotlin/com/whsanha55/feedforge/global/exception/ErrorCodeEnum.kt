package com.whsanha55.feedforge.global.exception

import org.springframework.http.HttpStatus

enum class ErrorCodeEnum(val status: HttpStatus, val message: String) {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값을 확인해주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "잠시 후 다시 시도해주세요."),
    EXTERNAL_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "외부 시스템 장애로 일시적으로 처리할 수 없습니다."),
}
