package com.whsanha55.feedforge

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = ["feedforge.security.access-token=test-access-token"])
@Testcontainers
class AuthSecurityTest @Autowired constructor(private val mockMvc: MockMvc) {

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:16"))
    }

    @Test
    fun `토큰 없이 보호된 API를 호출하면 401 problem 응답을 반환한다`() {
        // when
        val result = mockMvc.get("/feedforge/me")

        // then
        result.andExpect {
            status { isUnauthorized() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.code") { value("UNAUTHORIZED") }
            jsonPath("$.detail") { value("로그인이 필요합니다.") }
            jsonPath("$.requestId") { exists() }
        }
    }

    @Test
    fun `잘못된 토큰으로 보호된 API를 호출하면 401을 반환한다`() {
        // given
        val wrongToken = "Bearer wrong-token"

        // when
        val result = mockMvc.get("/feedforge/me") {
            header(HttpHeaders.AUTHORIZATION, wrongToken)
        }

        // then
        result.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `올바른 토큰으로 me를 호출하면 사용자 정보를 반환한다`() {
        // given
        val validToken = "Bearer test-access-token"

        // when
        val result = mockMvc.get("/feedforge/me") {
            header(HttpHeaders.AUTHORIZATION, validToken)
        }

        // then
        result.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.user") { value("owner") }
        }
    }

    @Test
    fun `헬스체크는 토큰 없이 호출할 수 있다`() {
        // when
        val result = mockMvc.get("/actuator/health")

        // then
        result.andExpect {
            status { isOk() }
        }
    }
}
