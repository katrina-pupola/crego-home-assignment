package com.crego.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Replaces the real mail sender (Mailpit/SMTP) in integration tests. */
@TestConfiguration(proxyBeanMethods = false)
public class TestMailConfig {

    @Bean
    RecordingMailSender mailSender() {
        return new RecordingMailSender();
    }
}
