package com.opticloud.normalization.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebClientConfig {

    @Value("${opticloud.reports.timeout-ms:5000}")
    private long timeoutMs;

    @Bean
    public HttpClient reportsHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }
}
