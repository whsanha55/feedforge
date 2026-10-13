package com.whsanha55.feedforge.global.security

import com.whsanha55.feedforge.global.constants.REQUEST_ID_MDC_KEY
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

private val log = KotlinLogging.logger {}

private const val BEARER_PREFIX = "Bearer "
private const val PRINCIPAL = "owner"

// 단일 사용자 정적 토큰 인증. 토큰 값 자체는 로그에 남기지 않는다.
class TokenAuthenticationFilter(private val accessToken: ByteArray) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = extractBearerToken(request)
        if (token == null) {
            filterChain.doFilter(request, response)
            return
        }

        // 타이밍 공격 대비 상수 시간 비교를 쓴다.
        if (MessageDigest.isEqual(token, accessToken)) {
            val authentication = UsernamePasswordAuthenticationToken.authenticated(PRINCIPAL, null, emptyList())
            SecurityContextHolder.getContext().authentication = authentication
        } else {
            log.warn { "유효하지 않은 액세스 토큰 접근. requestId=${MDC.get(REQUEST_ID_MDC_KEY)}" }
        }
        filterChain.doFilter(request, response)
    }

    private fun extractBearerToken(request: HttpServletRequest): ByteArray? =
        request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX, ignoreCase = true) }
            ?.substring(BEARER_PREFIX.length)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.toByteArray()
}
