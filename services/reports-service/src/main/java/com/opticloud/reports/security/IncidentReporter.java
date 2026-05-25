package com.opticloud.reports.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Handles a blocked cross-company access to a report resource: persists the
 * attempt to MongoDB and notifies the notification-service. Runs asynchronously
 * so the 403 returned by CompanyAuthFilter is not delayed (keeps the block
 * within the < 500 ms budget; the log + notification land within ~1 s).
 */
@Service
public class IncidentReporter {

    private static final Logger log = LoggerFactory.getLogger(IncidentReporter.class);

    private final AccessLogRepository accessLogRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).build();

    @Value("${opticloud.notification.base-url:http://localhost:8080/notifications}")
    private String notificationBaseUrl;

    public IncidentReporter(AccessLogRepository accessLogRepository, ObjectMapper objectMapper) {
        this.accessLogRepository = accessLogRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    public void reportCompanyMismatch(String userId, String tokenCompany, String requestedCompany,
                                      String sourceIp, String path, String method) {
        Map<String, Object> context = new HashMap<>();
        context.put("requestedCompany", requestedCompany);

        // 1) persist evidence to MongoDB (Access Log)
        try {
            accessLogRepository.save(new UnauthorizedAccessLog(
                    "COMPANY_MISMATCH", userId, tokenCompany, sourceIp, path, method, context));
        } catch (Exception ex) {
            log.warn("Could not persist unauthorized access log: {}", ex.getMessage());
        }

        // 2) notify the notification-service (Security Incident Handler + Email)
        try {
            Map<String, Object> incident = new LinkedHashMap<>();
            incident.put("incident_type", "COMPANY_MISMATCH");
            incident.put("company_id", tokenCompany);
            incident.put("user_id", userId);
            incident.put("source_ip", sourceIp);
            incident.put("path", path);
            incident.put("detail", "Cross-company report access blocked by reports-service");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(notificationBaseUrl + "/incidents"))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(incident)))
                    .build();
            http.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ex) {
            log.warn("Could not deliver security incident to notification-service: {}", ex.getMessage());
        }
    }
}
