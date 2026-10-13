package com.whsanha55.feedforge.global.base

import com.whsanha55.feedforge.global.exception.ErrorCodeEnum

open class BaseException(val errorCode: ErrorCodeEnum) : RuntimeException(errorCode.message)
