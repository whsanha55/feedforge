package com.whsanha55.feedforge.global.exception

import com.whsanha55.feedforge.global.base.BaseException
import com.whsanha55.feedforge.global.constants.REQUEST_ID_MDC_KEY
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

private val log = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(BaseException::class)
    fun handleBaseException(e: BaseException): ProblemDetail = problemOf(e.errorCode)

    @ExceptionHandler(Exception::class)
    fun handleUnknown(e: Exception): ProblemDetail {
        log.error(e) { "처리되지 않은 예외" }
        return problemOf(ErrorCodeEnum.INTERNAL_ERROR)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val problem = problemOf(ErrorCodeEnum.INVALID_REQUEST)
        problem.setProperty(
            "errors",
            ex.bindingResult.fieldErrors.map { fieldError ->
                mapOf("field" to fieldError.field, "message" to fieldError.defaultMessage)
            },
        )
        return handleExceptionInternal(ex, problem, headers, status, request)
    }

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        if (body is ProblemDetail) {
            body.setProperty("code", errorCodeOf(statusCode).name)
            body.setProperty("requestId", MDC.get(REQUEST_ID_MDC_KEY))
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request)
    }

    private fun problemOf(errorCode: ErrorCodeEnum): ProblemDetail =
        ProblemDetail.forStatusAndDetail(errorCode.status, errorCode.message).apply {
            setProperty("code", errorCode.name)
            setProperty("requestId", MDC.get(REQUEST_ID_MDC_KEY))
        }

    private fun errorCodeOf(status: HttpStatusCode): ErrorCodeEnum = when (status.value()) {
        HttpStatus.UNAUTHORIZED.value() -> ErrorCodeEnum.UNAUTHORIZED
        HttpStatus.FORBIDDEN.value() -> ErrorCodeEnum.FORBIDDEN
        HttpStatus.NOT_FOUND.value() -> ErrorCodeEnum.NOT_FOUND
        else -> if (status.is4xxClientError) ErrorCodeEnum.INVALID_REQUEST else ErrorCodeEnum.INTERNAL_ERROR
    }
}
