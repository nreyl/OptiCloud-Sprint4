package com.opticloud.reports.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findByCompanyCodeOrderByPeriodStartDesc(String companyCode);

    @Query("""
           select r from Report r
            where r.companyCode = :company
              and (:provider is null or r.provider = :provider)
              and r.periodStart >= :from
              and r.periodEnd   <= :to
           """)
    List<Report> search(@Param("company") String company,
                        @Param("provider") String provider,
                        @Param("from") OffsetDateTime from,
                        @Param("to") OffsetDateTime to);
}
