package com.whsanha55.feedforge.global.filter

import com.whsanha55.feedforge.global.constants.REQUEST_ID_HEADER
import com.whsanha55.feedforge.global.constants.REQUEST_ID_MDC_KEY
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class RequestIdFilterTest {

    private val filter = RequestIdFilter()

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `요청 헤더에 X-Request-Id가 있으면 해당 값을 재사용한다`() {
        // given
        val requestIds = mutableListOf<String?>()
        val request = MockHttpServletRequest().apply { addHeader(REQUEST_ID_HEADER, "given-request-id") }
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, _ -> requestIds.add(MDC.get(REQUEST_ID_MDC_KEY)) }

        // when
        filter.doFilter(request, response, chain)

        // then
        assertThat(requestIds).containsExactly("given-request-id")
        assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo("given-request-id")
    }

    @Test
    fun `요청 헤더에 X-Request-Id가 없으면 UUID 식별자를 생성한다`() {
        // given
        val requestIds = mutableListOf<String?>()
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, _ -> requestIds.add(MDC.get(REQUEST_ID_MDC_KEY)) }

        // when
        filter.doFilter(request, response, chain)

        // then
        val requestId = requireNotNull(requestIds.single())
        assertThat(requestId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo(requestId)
    }

    @Test
    fun `필터 처리가 끝나면 MDC에서 requestId가 제거된다`() {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, _ -> }

        // when
        filter.doFilter(request, response, chain)

        // then
        assertThat(MDC.get(REQUEST_ID_MDC_KEY)).isNull()
    }

    @Test
    fun `최우선 순서로 등록된다`() {
        assertThat(filter.order).isEqualTo(Ordered.HIGHEST_PRECEDENCE)
    }
}
