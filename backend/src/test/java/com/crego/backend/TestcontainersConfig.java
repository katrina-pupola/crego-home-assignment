package com.crego.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Starts a throwaway Postgres 17 in Docker for the tests; Flyway migrates it on startup. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection // points the datasource at this container instead of application.yml
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17");
    }
}
