package com.whsanha55.feedforge.collect

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "feedforge.collect")
data class CollectProperties(val zod: Zod = Zod()) {
    data class Zod(val enabled: Boolean = true, val baseUrl: String = "https://zod.kr")
}
