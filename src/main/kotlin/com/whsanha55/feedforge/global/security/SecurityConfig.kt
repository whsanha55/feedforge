package com.whsanha55.feedforge.global.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.whsanha55.feedforge.global.constants.REQUEST_ID_MDC_KEY
import com.whsanha55.feedforge.global.exception.ErrorCodeEnum
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@EnableWebSecurity
@Configuration
@EnableConfigurationProperties(SecurityProperties::class)
class SecurityConfig(private val objectMapper: ObjectMapper) {

    @Bean
    fun securityFilterChain(http: HttpSecurity, properties: SecurityProperties): SecurityFilterChain {
        http {
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                authorize("/actuator/health", permitAll)
                authorize(anyRequest, authenticated)
            }
            addFilterBefore(
                TokenAuthenticationFilter(properties.accessToken.toByteArray()),
                UsernamePasswordAuthenticationFilter::class.java,
            )
            exceptionHandling { authenticationEntryPoint = unauthorizedEntryPoint() }
        }
        return http.build()
    }

    private fun unauthorizedEntryPoint(): AuthenticationEntryPoint = AuthenticationEntryPoint {
            request: HttpServletRequest,
            response: HttpServletResponse,
            authException: AuthenticationException,
        ->
        response.status = ErrorCodeEnum.UNAUTHORIZED.status.value()
        response.characterEncoding = Charsets.UTF_8.name()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        val problem = ProblemDetail.forStatusAndDetail(
            ErrorCodeEnum.UNAUTHORIZED.status,
            ErrorCodeEnum.UNAUTHORIZED.message,
        )
        problem.setProperty("code", ErrorCodeEnum.UNAUTHORIZED.name)
        problem.setProperty("requestId", MDC.get(REQUEST_ID_MDC_KEY))
        response.writer.use { writer -> writer.write(objectMapper.writeValueAsString(problem)) }
    }
}
