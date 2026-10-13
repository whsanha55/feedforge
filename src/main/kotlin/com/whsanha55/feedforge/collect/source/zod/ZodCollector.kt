package com.whsanha55.feedforge.collect.source.zod

import com.whsanha55.feedforge.collect.CollectProperties
import com.whsanha55.feedforge.collect.exception.CollectException
import com.whsanha55.feedforge.global.exception.ErrorCodeEnum
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

private val log = KotlinLogging.logger {}

// zod.kr/deal 목록을 가져와 파싱한다. 실제 요청은 Cloudflare 차단 가능성이 있어
// 런타임 실패 시 설정(enabled=false)으로 소스를 중단한다 (PLAN 13절 방침).
@Component
class ZodCollector(private val restClient: RestClient, private val properties: CollectProperties) {

    fun fetchDeals(): List<ZodDeal> {
        val html = restClient.get()
            .uri("${properties.zod.baseUrl}/deal")
            .accept(MediaType.TEXT_HTML)
            .retrieve()
            .onStatus(HttpStatusCode::isError) { request, response ->
                log.warn {
                    "zod 수집 소스 오류 응답. status=${response.statusCode}, requestUrl=$request"
                }
                throw CollectException(ErrorCodeEnum.EXTERNAL_API_ERROR)
            }
            .body(String::class.java)
            ?: throw CollectException(ErrorCodeEnum.EXTERNAL_API_ERROR)
        return ZodParser.parse(html)
    }
}
