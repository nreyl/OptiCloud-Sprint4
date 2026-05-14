package com.opticloud.normalization.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.opticloud.normalization.forward.ReportForwarder;
import com.opticloud.normalization.normalize.NormalizedReport;
import com.opticloud.normalization.normalize.Normalizer;
import com.opticloud.normalization.validation.JsonSchemaValidator;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/normalize")
public class NormalizationController {

    private static final Logger log = LoggerFactory.getLogger(NormalizationController.class);

    private final JsonSchemaValidator validator;
    private final Normalizer normalizer;
    private final ReportForwarder forwarder;

    public NormalizationController(JsonSchemaValidator validator, Normalizer normalizer, ReportForwarder forwarder) {
        this.validator = validator;
        this.normalizer = normalizer;
        this.forwarder = forwarder;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "normalization-service");
    }

    @PostMapping("/reports")
    public ResponseEntity<?> normalize(@RequestBody JsonNode payload) {
        var outcome = validator.validate(payload);
        if (!outcome.valid()) {
            log.warn("Schema validation failed: {}", outcome.errors());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "VALIDATION_FAILED",
                    "errors", outcome.errors()));
        }

        NormalizedReport report;
        try {
            report = normalizer.normalize(payload);
        } catch (IllegalArgumentException exc) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "UNSUPPORTED_PROVIDER",
                    "errors", List.of(exc.getMessage())));
        }

        var result = forwarder.forward(report);
        if (!result.ok()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "status", "FORWARD_FAILED",
                    "downstreamStatus", result.status(),
                    "downstreamBody", result.body()));
        }
        return ResponseEntity.accepted().body(Map.of(
                "status", "FORWARDED",
                "lines", report.lines().size(),
                "downstream", result.body()));
    }
}
