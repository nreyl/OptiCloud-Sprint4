package com.opticloud.normalization.forward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticloud.normalization.normalize.NormalizedReport;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReportForwarder {

    private static final Logger log = LoggerFactory.getLogger(ReportForwarder.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${opticloud.reports.base-url}")
    private String reportsBaseUrl;

    @Value("${opticloud.reports.timeout-ms:5000}")
    private long timeoutMs;

    public ReportForwarder(HttpClient reportsHttpClient, ObjectMapper objectMapper) {
        this.httpClient = reportsHttpClient;
        this.objectMapper = objectMapper;
    }

    public ForwardResult forward(NormalizedReport report) {
        try {
            String body = objectMapper.writeValueAsString(report);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(reportsBaseUrl + "/commands/reports"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("reports-service rejected forward: {} {}", response.statusCode(), response.body());
                return new ForwardResult(false, response.statusCode(), response.body());
            }
            return new ForwardResult(true, response.statusCode(), response.body());
        } catch (Exception exc) {
            log.error("Failed to forward report", exc);
            return new ForwardResult(false, 0, exc.getMessage());
        }
    }

    public record ForwardResult(boolean ok, int status, String body) {}
}
