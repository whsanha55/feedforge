package com.whsanha55.feedforge.global.exception

import com.fasterxml.jackson.databind.ObjectMapper
import com.whsanha55.feedforge.global.constants.REQUEST_ID_HEADER
import com.whsanha55.feedforge.global.filter.RequestIdFilter
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

private data class TestRequest(
    @field:NotBlank
    val name: String,
)

@RestController
private class ValidationTestController {

    @PostMapping("/feedforge/test/validation")
    fun validate(@RequestBody @Valid request: TestRequest): TestRequest = request
}

class GlobalExceptionHandlerTest {

    private val mockMvc: MockMvc by lazy {
        MockMvcBuilders.standaloneSetup(ValidationTestController())
            .setControllerAdvice(GlobalExceptionHandler())
            .addFilters<StandaloneMockMvcBuilder>(RequestIdFilter())
            .build()
    }

    @Test
    fun `Bean Validation 실패 시 400과 errors 배열을 problem+json으로 응답한다`() {
        // given
        val requestBody = """{"name":""}"""

        // when
        val result = mockMvc.perform(
            post("/feedforge/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(status().isBadRequest)
            .andReturn()

        // then
        val response = result.response
        assertThat(response.getHeader("Content-Type")).startsWith("application/problem+json")
        assertThat(response.getHeader(REQUEST_ID_HEADER)).isNotBlank()

        val body = ObjectMapper().readTree(response.contentAsString)
        assertThat(body.get("code").asText()).isEqualTo("INVALID_REQUEST")
        assertThat(body.get("requestId").asText()).isEqualTo(response.getHeader(REQUEST_ID_HEADER))
        assertThat(body.at("/errors/0/field").asText()).isEqualTo("name")
        assertThat(body.at("/errors/0/message").asText()).isNotBlank()
    }
}
