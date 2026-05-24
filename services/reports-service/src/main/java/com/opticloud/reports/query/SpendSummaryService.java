package com.opticloud.reports.query;

import java.time.ZoneOffset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Owns the `company_spend_summary` PostgreSQL **materialized view** that
 * pre-calculates monthly spend per company/provider. The read side
 * (QueryService) serves from it; the write side (CommandService) refreshes it
 * after each persisted report — that is the CQRS "pre-calculated views"
 * behaviour the architecture promises.
 */
@Service
public class SpendSummaryService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SpendSummaryService.class);

    private static final String CREATE_VIEW = """
            CREATE MATERIALIZED VIEW IF NOT EXISTS company_spend_summary AS
            SELECT r.company_code                      AS company_code,
                   r.provider                          AS provider,
                   date_trunc('month', r.period_start) AS period_month,
                   count(DISTINCT r.id)                AS report_count,
                   count(l.id)                         AS line_count,
                   coalesce(sum(l.cost_amount), 0)     AS total_cost
              FROM reports r
              LEFT JOIN report_lines l ON l.report_id = r.id
             GROUP BY r.company_code, r.provider, date_trunc('month', r.period_start)
            """;

    private final JdbcTemplate jdbc;

    public SpendSummaryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Create the materialized view once the JPA tables exist (runs at startup). */
    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute(CREATE_VIEW);
            log.info("Materialized view company_spend_summary is ready");
        } catch (Exception ex) {
            log.warn("Could not create company_spend_summary view: {}", ex.getMessage());
        }
    }

    /** Re-run the aggregation. Called by CommandService after every write. */
    public void refresh() {
        try {
            jdbc.execute("REFRESH MATERIALIZED VIEW company_spend_summary");
        } catch (Exception ex) {
            log.warn("Could not refresh company_spend_summary view: {}", ex.getMessage());
        }
    }

    @Cacheable(value = "reports-queries", key = "'spend:' + #company")
    public List<SpendSummaryView> byCompany(String company) {
        return jdbc.query(
                """
                SELECT company_code, provider, period_month, report_count, line_count, total_cost
                  FROM company_spend_summary
                 WHERE company_code = ?
                 ORDER BY period_month DESC, provider
                """,
                (rs, i) -> new SpendSummaryView(
                        rs.getString("company_code"),
                        rs.getString("provider"),
                        rs.getTimestamp("period_month").toInstant().atOffset(ZoneOffset.UTC),
                        rs.getLong("report_count"),
                        rs.getLong("line_count"),
                        rs.getDouble("total_cost")),
                company);
    }
}
