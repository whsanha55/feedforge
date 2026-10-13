package com.whsanha55.feedforge.feed.enums

import org.springframework.data.domain.Sort

enum class FeedSortEnum {
    RECOMMENDED,
    LATEST,
}

fun FeedSortEnum.toSort(): Sort = when (this) {
    FeedSortEnum.RECOMMENDED -> Sort.by(Sort.Direction.DESC, "score")
    FeedSortEnum.LATEST -> Sort.by(Sort.Direction.DESC, "rawItem.itemAt")
}
