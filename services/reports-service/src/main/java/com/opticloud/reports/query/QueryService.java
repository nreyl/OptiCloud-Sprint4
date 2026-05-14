package com.opticloud.reports.query;

import com.opticloud.reports.domain.Report;
import com.opticloud.reports.domain.ReportRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QueryService {

    private final ReportRepository repository;

    public QueryService(ReportRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "reports-queries", key = "'byId:' + #id")
    public Optional<ReportView> findById(UUID id) {
        return repository.findById(id).map(ReportView::from);
    }

    @Cacheable(value = "reports-queries", key = "'byCompany:' + #company")
    public List<ReportView> findByCompany(String company) {
        return repository.findByCompanyCodeOrderByPeriodStartDesc(company)
                .stream()
                .map(ReportView::from)
                .toList();
    }

    @Cacheable(
            value = "reports-queries",
            key = "'search:' + #company + ':' + #provider + ':' + #from + ':' + #to")
    public List<ReportView> search(String company, String provider, OffsetDateTime from, OffsetDateTime to) {
        return repository.search(company, provider, from, to)
                .stream()
                .map(ReportView::from)
                .toList();
    }

    @Cacheable(value = "reports-queries", key = "'summary:' + #company + ':' + #from + ':' + #to")
    public CompanySummary summary(String company, OffsetDateTime from, OffsetDateTime to) {
        List<Report> reports = repository.search(company, null, from, to);
        double total = reports.stream().mapToDouble(Report::totalCost).sum();
        long lines = reports.stream().mapToLong(r -> r.getLines().size()).sum();
        return new CompanySummary(company, from, to, reports.size(), lines, total);
    }

    public record CompanySummary(
            String companyCode,
            OffsetDateTime from,
            OffsetDateTime to,
            int reportCount,
            long lineCount,
            double totalCost) implements java.io.Serializable {}
}
