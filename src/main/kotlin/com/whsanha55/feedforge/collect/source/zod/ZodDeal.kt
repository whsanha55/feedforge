package com.whsanha55.feedforge.collect.source.zod

import java.time.Instant

data class ZodDeal(
    val sourceKey: String,
    val title: String,
    val url: String,
    val mall: String?,
    val price: Long?,
    val shippingFee: String?,
    val itemAt: Instant,
    val votes: Long,
)
