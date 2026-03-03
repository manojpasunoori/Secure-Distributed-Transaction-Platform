package com.sdtp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(PlatformProperties.class)
public class AppConfig {
    @Bean(destroyMethod = "shutdown")
    public ExecutorService validatorExecutor() {
        return Executors.newFixedThreadPool(16);
    }
}
