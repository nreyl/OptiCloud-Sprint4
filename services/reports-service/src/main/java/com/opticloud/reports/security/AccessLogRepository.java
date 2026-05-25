package com.opticloud.reports.security;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AccessLogRepository extends MongoRepository<UnauthorizedAccessLog, String> {
}
