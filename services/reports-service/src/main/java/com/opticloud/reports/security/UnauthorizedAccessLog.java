package com.opticloud.reports.security;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Evidence of a blocked cross-company access attempt, written to the same
 * `unauthorized_access_logs` MongoDB collection that auth-service uses — so the
 * ASR 2 mongosh query surfaces attempts against report resources too.
 */
@Document(collection = "unauthorized_access_logs")
public class UnauthorizedAccessLog {

    @Id
    private String id;
    private String reason;
    private String userId;
    private String companyId;
    private String sourceIp;
    private String path;
    private String method;
    private Map<String, Object> context;
    private OffsetDateTime occurredAt;

    public UnauthorizedAccessLog() {}

    public UnauthorizedAccessLog(String reason, String userId, String companyId, String sourceIp,
                                 String path, String method, Map<String, Object> context) {
        this.reason = reason;
        this.userId = userId;
        this.companyId = companyId;
        this.sourceIp = sourceIp;
        this.path = path;
        this.method = method;
        this.context = context;
        this.occurredAt = OffsetDateTime.now();
    }

    public String getId() { return id; }
    public String getReason() { return reason; }
    public String getUserId() { return userId; }
    public String getCompanyId() { return companyId; }
    public String getSourceIp() { return sourceIp; }
    public String getPath() { return path; }
    public String getMethod() { return method; }
    public Map<String, Object> getContext() { return context; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
}
