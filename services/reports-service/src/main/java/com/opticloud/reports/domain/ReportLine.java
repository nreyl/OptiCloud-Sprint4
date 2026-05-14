package com.opticloud.reports.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "report_lines")
public class ReportLine {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Column(nullable = false, length = 200)
    private String service;

    @Column(length = 256)
    private String resourceId;

    @Column(length = 64)
    private String region;

    @Column(nullable = false)
    private double usageAmount;

    @Column(nullable = false, length = 32)
    private String usageUnit;

    @Column(nullable = false)
    private double costAmount;

    @Column(nullable = false, length = 8)
    private String costCurrency;

    protected ReportLine() {}

    public ReportLine(String service, String resourceId, String region,
                      double usageAmount, String usageUnit,
                      double costAmount, String costCurrency) {
        this.service = service;
        this.resourceId = resourceId;
        this.region = region;
        this.usageAmount = usageAmount;
        this.usageUnit = usageUnit;
        this.costAmount = costAmount;
        this.costCurrency = costCurrency;
    }

    public UUID getId() { return id; }
    public Report getReport() { return report; }
    void setReport(Report report) { this.report = report; }
    public String getService() { return service; }
    public String getResourceId() { return resourceId; }
    public String getRegion() { return region; }
    public double getUsageAmount() { return usageAmount; }
    public String getUsageUnit() { return usageUnit; }
    public double getCostAmount() { return costAmount; }
    public String getCostCurrency() { return costCurrency; }
}
