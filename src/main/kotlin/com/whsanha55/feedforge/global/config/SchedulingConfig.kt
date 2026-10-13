package com.whsanha55.feedforge.global.config

import com.whsanha55.feedforge.collect.CollectProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@Configuration
@EnableConfigurationProperties(CollectProperties::class)
class SchedulingConfig
