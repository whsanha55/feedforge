package com.whsanha55.feedforge.collect.source.zod

import com.whsanha55.feedforge.collect.CollectProperties
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

class ZodCollectorTest {

    @Test
    fun `받아온 HTML을 파싱해 딜 목록을 반환한다`() {
        // given
        val html = """
            <a class="app-list-link" href="/deal/9">
              <div class="app-list-body">
                <div class="app-list-title"><span class="app-list-title-item">SSD 딜</span></div>
                <dl class="app-list-meta zod-board--deal-meta">
                  <dt>홈페이지/장소</dt><dd><strong>쿠팡</strong></dd>
                  <dt>가격</dt><dd>가격: <strong>50,000원</strong></dd>
                  <dt>배송비</dt><dd>배송비: <strong>무료</strong></dd>
                </dl>
                <dl class="app-list-meta">
                  <dd title="2026-10-13 11:00:00"><i class="iconoir-calendar"></i>1시간 전</dd>
                  <dd class="app-list__voted-count"><i class="iconoir-heart"></i>5</dd>
                </dl>
              </div>
            </a>
        """.trimIndent()
        val restClient = mockk<RestClient>()
        val headersSpec = mockk<RestClient.RequestHeadersSpec<*>>()
        every { restClient.get() } returns mockk {
            every { uri("https://zod.kr/deal") } returns headersSpec
        }
        every { headersSpec.accept(MediaType.TEXT_HTML) } returns headersSpec
        every { headersSpec.retrieve() } returns mockk {
            every { onStatus(any(), any()) } returns this@mockk
            every { body(String::class.java) } returns html
        }
        val collector = ZodCollector(restClient, CollectProperties())

        // when
        val deals = collector.fetchDeals()

        // then
        assertThat(deals).hasSize(1)
        assertThat(deals.single().title).isEqualTo("SSD 딜")
    }
}
