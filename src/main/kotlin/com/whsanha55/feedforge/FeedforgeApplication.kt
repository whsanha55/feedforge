package com.whsanha55.feedforge

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FeedforgeApplication

// Boot 표준 진입점 관용구다. main 인자 전달은 요청당 1회라 배열 복사 비용이 의미 없다.
@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<FeedforgeApplication>(*args)
}
