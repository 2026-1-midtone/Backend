package com.midtone.backend.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    private static final String MYSQL_IMAGE = "mysql:8.4";
    private static final String REDIS_IMAGE = "redis:7.4-alpine";
    private static final String REDIS_SERVICE_NAME = "redis";
    private static final int REDIS_PORT = 6379;

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse(MYSQL_IMAGE));
    }

    @Bean
    @ServiceConnection(name = REDIS_SERVICE_NAME)
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE)).withExposedPorts(REDIS_PORT);
    }
}
