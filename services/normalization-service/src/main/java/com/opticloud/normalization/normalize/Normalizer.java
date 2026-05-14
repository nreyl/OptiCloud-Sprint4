package com.opticloud.normalization.normalize;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Normalizer {

    public NormalizedReport normalize(JsonNode raw) {
        String provider = raw.path("provider").asText();
        return switch (provider) {
            case "aws" -> normalizeAws(raw);
            case "azure" -> normalizeAzure(raw);
            case "gcp" -> normalizeGcp(raw);
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }

    private NormalizedReport normalizeAws(JsonNode raw) {
        List<NormalizedReport.NormalizedLine> lines = new ArrayList<>();
        for (JsonNode line : raw.path("rawLines")) {
            JsonNode cost = line.path("UnblendedCost");
            lines.add(new NormalizedReport.NormalizedLine(
                    line.path("Service").asText(""),
                    line.path("ResourceId").asText(""),
                    line.path("Region").asText(""),
                    line.path("UsageQuantity").asDouble(0.0),
                    line.path("UsageUnit").asText(""),
                    cost.path("Amount").asDouble(0.0),
                    cost.path("Currency").asText("USD")));
        }
        return baseReport(raw, lines);
    }

    private NormalizedReport normalizeAzure(JsonNode raw) {
        List<NormalizedReport.NormalizedLine> lines = new ArrayList<>();
        for (JsonNode line : raw.path("rawLines")) {
            lines.add(new NormalizedReport.NormalizedLine(
                    line.path("serviceName").asText(""),
                    line.path("resourceId").asText(""),
                    line.path("resourceLocation").asText(""),
                    line.path("quantity").asDouble(0.0),
                    line.path("unitOfMeasure").asText(""),
                    line.path("pretaxCost").asDouble(0.0),
                    line.path("billingCurrency").asText("USD")));
        }
        return baseReport(raw, lines);
    }

    private NormalizedReport normalizeGcp(JsonNode raw) {
        List<NormalizedReport.NormalizedLine> lines = new ArrayList<>();
        for (JsonNode line : raw.path("rawLines")) {
            lines.add(new NormalizedReport.NormalizedLine(
                    line.path("service").path("description").asText(""),
                    line.path("resource").path("name").asText(""),
                    line.path("location").path("region").asText(""),
                    line.path("usage").path("amount").asDouble(0.0),
                    line.path("usage").path("unit").asText(""),
                    line.path("cost").asDouble(0.0),
                    line.path("currency").asText("USD")));
        }
        return baseReport(raw, lines);
    }

    private NormalizedReport baseReport(JsonNode raw, List<NormalizedReport.NormalizedLine> lines) {
        return new NormalizedReport(
                raw.path("companyCode").asText(),
                raw.path("provider").asText(),
                raw.path("accountId").asText(""),
                OffsetDateTime.parse(raw.path("periodStart").asText()),
                OffsetDateTime.parse(raw.path("periodEnd").asText()),
                raw.path("source").asText("CLOUD_ADAPTER"),
                lines);
    }
}
