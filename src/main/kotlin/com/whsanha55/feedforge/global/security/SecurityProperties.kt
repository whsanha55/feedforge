package com.whsanha55.feedforge.global.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "feedforge.security")
data class SecurityProperties(val accessToken: String) {
    init {
        // 운영에서 환경 변수 누락을 모르고 띄우는 것보다 기동 시점에 실패하는 것이 안전하다.
        require(accessToken.isNotBlank()) {
            "feedforge.security.access-token이 비어 있다. FEEDFORGE_ACCESS_TOKEN 환경 변수를 설정한다."
        }
    }
}
