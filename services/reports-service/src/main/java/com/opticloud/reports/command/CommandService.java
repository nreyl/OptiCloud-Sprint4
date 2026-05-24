package com.opticloud.reports.command;

import com.opticloud.reports.domain.Report;
import com.opticloud.reports.domain.ReportLine;
import com.opticloud.reports.domain.ReportRepository;
import com.opticloud.reports.query.SpendSummaryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommandService {

    private static final Logger log = LoggerFactory.getLogger(CommandService.class);

    private final ReportRepository repository;
    private final SpendSummaryService spendSummary;

    public CommandService(ReportRepository repository, SpendSummaryService spendSummary) {
        this.repository = repository;
        this.spendSummary = spendSummary;
    }

    @Transactional
    @CacheEvict(value = "reports-queries", allEntries = true)
    public Report createReport(CreateReportCommand cmd) {
        Report report = new Report(
                cmd.companyCode(),
                cmd.provider(),
                cmd.accountId(),
                cmd.periodStart(),
                cmd.periodEnd(),
                cmd.source() != null ? cmd.source() : "UNKNOWN");

        for (CreateReportCommand.Line line : cmd.lines()) {
            report.addLine(new ReportLine(
                    line.service(),
                    line.resourceId(),
                    line.region(),
                    line.usageAmount(),
                    line.usageUnit(),
                    line.costAmount(),
                    line.costCurrency() != null ? line.costCurrency() : "USD"));
        }
        Report saved = repository.save(report);
        log.info("Stored report id={} company={} provider={} lines={}",
                saved.getId(), saved.getCompanyCode(), saved.getProvider(), saved.getLines().size());
        spendSummary.refresh();
        return saved;
    }

    @Transactional
    @CacheEvict(value = "reports-queries", allEntries = true)
    public void deleteReport(java.util.UUID id) {
        repository.deleteById(id);
        spendSummary.refresh();
    }
}
