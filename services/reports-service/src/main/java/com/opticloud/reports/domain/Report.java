package com.opticloud.reports.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_reports_company", columnList = "companyCode"),
        @Index(name = "idx_reports_provider", columnList = "provider"),
        @Index(name = "idx_reports_period_start", columnList = "periodStart")
})
public class Report {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 64)
    private String companyCode;

    @Column(nullable = false, length = 16)
    private String provider;

    @Column(length = 128)
    private String accountId;

    @Column(nullable = false)
    private OffsetDateTime periodStart;

    @Column(nullable = false)
    private OffsetDateTime periodEnd;

    @Column(length = 32)
    private String source;

    @Column(nullable = false)
    private OffsetDateTime ingestedAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ReportLine> lines = new ArrayList<>();

    protected Report() {}

    public Report(String companyCode, String provider, String accountId,
                  OffsetDateTime periodStart, OffsetDateTime periodEnd, String source) {
        this.companyCode = companyCode;
        this.provider = provider;
        this.accountId = accountId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.source = source;
        this.ingestedAt = OffsetDateTime.now();
    }

    public void addLine(ReportLine line) {
        line.setReport(this);
        this.lines.add(line);
    }

    public UUID getId() { return id; }
    public String getCompanyCode() { return companyCode; }
    public String getProvider() { return provider; }
    public String getAccountId() { return accountId; }
    public OffsetDateTime getPeriodStart() { return periodStart; }
    public OffsetDateTime getPeriodEnd() { return periodEnd; }
    public String getSource() { return source; }
    public OffsetDateTime getIngestedAt() { return ingestedAt; }
    public List<ReportLine> getLines() { return lines; }

    public double totalCost() {
        return lines.stream().mapToDouble(ReportLine::getCostAmount).sum();
    }
}
