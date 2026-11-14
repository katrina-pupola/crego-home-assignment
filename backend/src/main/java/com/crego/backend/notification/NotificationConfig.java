package com.crego.backend.notification;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// @EnableAsync lets emails be sent on a background thread (Spring Boot's task executor)
@Configuration
@EnableAsync
@EnableConfigurationProperties(AppMailProperties.class)
public class NotificationConfig {
}
