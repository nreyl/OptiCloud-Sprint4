package com.opticloud.reports.query;

import java.io.Serializable;
import java.time.OffsetDateTime;

/** One pre-aggregated row of the company_spend_summary materialized view. */
public record SpendSummaryView(
        String companyCode,
        String provider,
        OffsetDateTime periodMonth,
        long reportCount,
        long lineCount,
        double totalCost) implements Serializable {}
