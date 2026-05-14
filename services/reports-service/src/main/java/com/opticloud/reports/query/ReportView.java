package com.opticloud.reports.query;

import com.opticloud.reports.domain.Report;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReportView(
        UUID id,
        String companyCode,
        String provider,
        String accountId,
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd,
        String source,
        OffsetDateTime ingestedAt,
        double totalCost,
        List<LineView> lines) implements Serializable {

    public record LineView(
            UUID id,
            String service,
            String resourceId,
            String region,
            double usageAmount,
            String usageUnit,
            double costAmount,
            String costCurrency) implements Serializable {}

    public static ReportView from(Report report) {
        List<LineView> lines = report.getLines().stream()
                .map(l -> new LineView(
                        l.getId(),
                        l.getService(),
                        l.getResourceId(),
                        l.getRegion(),
                        l.getUsageAmount(),
                        l.getUsageUnit(),
                        l.getCostAmount(),
                        l.getCostCurrency()))
                .toList();
        return new ReportView(
                report.getId(),
                report.getCompanyCode(),
                report.getProvider(),
                report.getAccountId(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                report.getSource(),
                report.getIngestedAt(),
                report.totalCost(),
                lines);
    }
}
