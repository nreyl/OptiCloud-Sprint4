package com.opticloud.reports.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

public record CreateReportCommand(
        @NotBlank String companyCode,
        @NotBlank String provider,
        String accountId,
        @NotNull OffsetDateTime periodStart,
        @NotNull OffsetDateTime periodEnd,
        String source,
        @NotEmpty @Valid List<Line> lines) {

    public record Line(
            @NotBlank String service,
            String resourceId,
            String region,
            double usageAmount,
            @NotBlank String usageUnit,
            double costAmount,
            String costCurrency) {}
}
