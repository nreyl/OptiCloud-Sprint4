package com.opticloud.reports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ReportsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportsServiceApplication.class, args);
    }
}
