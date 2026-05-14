package com.opticloud.reports.web;

import com.opticloud.reports.query.QueryService;
import com.opticloud.reports.query.ReportView;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports/queries")
public class QueryController {

    private final QueryService queries;

    public QueryController(QueryService queries) {
        this.queries = queries;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "reports-service");
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<ReportView> byId(@PathVariable UUID id) {
        return queries.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/companies/{company}/reports")
    public List<ReportView> byCompany(@PathVariable String company) {
        return queries.findByCompany(company);
    }

    @GetMapping("/reports")
    public List<ReportView> search(
            @RequestParam String company,
            @RequestParam(required = false) String provider,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return queries.search(company, provider, from, to);
    }

    @GetMapping("/companies/{company}/summary")
    public QueryService.CompanySummary summary(
            @PathVariable String company,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return queries.summary(company, from, to);
    }
}
