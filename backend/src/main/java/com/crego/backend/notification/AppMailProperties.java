package com.crego.backend.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.mail")
public record AppMailProperties(String from, String clientAppUrl, Duration verificationTtl) {
}
