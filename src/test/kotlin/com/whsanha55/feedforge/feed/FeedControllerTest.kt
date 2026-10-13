package com.whsanha55.feedforge.feed

import com.ninjasquad.springmockk.MockkBean
import com.whsanha55.feedforge.feed.dto.FeedItemPageResponse
import com.whsanha55.feedforge.feed.dto.FeedItemResponse
import com.whsanha55.feedforge.feed.enums.FeedSortEnum
import com.whsanha55.feedforge.feed.service.FeedService
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant

@WebMvcTest(FeedController::class)
@AutoConfigureMockMvc(addFilters = false)
class FeedControllerTest @Autowired constructor(private val mockMvc: MockMvc) {

    // @MockkBean 주입을 위해 var여야 한다.
    @Suppress("VarCouldBeVal")
    @MockkBean
    private lateinit var feedService: FeedService

    @Test
    fun `기본 파라미터로 피드를 조회한다`() {
        // given
        every { feedService.getFeedItems(FeedSortEnum.RECOMMENDED, 0, 20) } returns pageResponse()

        // when / then
        mockMvc.get("/feedforge/feed-items").andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.totalElements") { value(1) }
            jsonPath("$.items[0].title") { value("테스트 딜") }
            jsonPath("$.items[0].reason") { value("관심 매칭(SSD)") }
        }
    }

    @Test
    fun `정렬과 페이지 파라미터를 전달한다`() {
        // given
        every { feedService.getFeedItems(FeedSortEnum.LATEST, 2, 5) } returns pageResponse()

        // when / then
        mockMvc.get("/feedforge/feed-items") {
            param("sort", "LATEST")
            param("page", "2")
            param("size", "5")
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `지원하지 않는 정렬 값은 400 problem 응답을 반환한다`() {
        // when / then
        mockMvc.get("/feedforge/feed-items") {
            param("sort", "POPULAR")
        }.andExpect {
            status { isBadRequest() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.code") { value("INVALID_REQUEST") }
        }
    }

    private fun pageResponse() = FeedItemPageResponse(
        items = listOf(
            FeedItemResponse(
                id = 1L,
                source = "ZOD",
                title = "테스트 딜",
                url = "https://zod.kr/deal/1",
                mall = "지마켓",
                price = 543720L,
                shippingFee = "무료",
                itemAt = Instant.parse("2026-10-13T06:00:00Z"),
                recommendedAt = Instant.parse("2026-10-13T06:10:00Z"),
                score = 3.5,
                popular = false,
                reason = "관심 매칭(SSD)",
                matchedTerms = listOf(
                    FeedItemResponse.MatchedTermResponse(kind = "KEYWORD", value = "SSD", weight = 2.0),
                ),
            ),
        ),
        page = 0,
        size = 20,
        totalElements = 1L,
    )
}
