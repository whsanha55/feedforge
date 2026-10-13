package com.whsanha55.feedforge.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

// 외부 수집 소스는 응답이 느리거나 걸리는 경우가 많아 짧은 타임아웃으로 보호한다.
private val CONNECT_TIMEOUT = Duration.ofSeconds(5)
private val READ_TIMEOUT = Duration.ofSeconds(10)

@Configuration
class RestClientConfig {

    @Bean
    fun restClient(builder: RestClient.Builder): RestClient {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(CONNECT_TIMEOUT)
        factory.setReadTimeout(READ_TIMEOUT)
        return builder
            .requestFactory(factory)
            .build()
    }
}
