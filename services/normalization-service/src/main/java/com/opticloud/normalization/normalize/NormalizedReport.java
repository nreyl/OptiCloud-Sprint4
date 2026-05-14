package com.opticloud.normalization.normalize;

import java.time.OffsetDateTime;
import java.util.List;

public record NormalizedReport(
        String companyCode,
        String provider,
        String accountId,
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd,
        String source,
        List<NormalizedLine> lines) {

    public record NormalizedLine(
            String service,
            String resourceId,
            String region,
            double usageAmount,
            String usageUnit,
            double costAmount,
            String costCurrency) {}
}
