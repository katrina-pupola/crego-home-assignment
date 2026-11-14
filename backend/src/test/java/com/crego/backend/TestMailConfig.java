package com.crego.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/** Replaces the real mail sender (Mailpit/SMTP) in integration tests. */
@TestConfiguration(proxyBeanMethods = false)
public class TestMailConfig {

    @Bean
    RecordingMailSender mailSender() {
        return new RecordingMailSender();
    }

    // Runs @Async email sending on the test thread, so tests can check the email right after a request
    @Bean
    TaskExecutor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
