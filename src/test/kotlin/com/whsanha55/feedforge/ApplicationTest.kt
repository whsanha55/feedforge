package com.whsanha55.feedforge

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApplicationTest @Autowired constructor(
    private val applicationContext: ApplicationContext,
    private val restTemplate: TestRestTemplate,
) {

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:16"))
    }

    @Test
    fun `애플리케이션 컨텍스트가 정상적으로 로드된다`() {
        // then
        assertThat(applicationContext.containsBean("feedforgeApplication")).isTrue()
    }

    @Test
    fun `헬스체크 엔드포인트가 200과 UP을 응답한다`() {
        // when
        val response = restTemplate.getForEntity("/actuator/health", String::class.java)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("\"status\":\"UP\"")
    }
}
