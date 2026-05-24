package com.opticloud.reports.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces tenant isolation on the company-scoped report queries (Flujo 1):
 * the request must carry a valid JWT issued by auth-service, and the
 * {company} segment in the path must match the companyCode claim in the token.
 *
 * The JWT is verified locally with the shared HMAC secret — no call to
 * auth-service — which is what keeps the query within the 100 ms budget.
 * Command endpoints (/reports/commands/**) are reached only by internal
 * services (data-injestion, normalization-service) and are not filtered here.
 */
@Component
public class CompanyAuthFilter extends OncePerRequestFilter {

    private static final Pattern COMPANY_PATH =
            Pattern.compile("^/reports/queries/companies/([^/]+)(/.*)?$");

    private final SecretKey key;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CompanyAuthFilter(@Value("${opticloud.jwt.secret}") String secret) {
        // SecretKeySpec (instead of Keys.hmacShaKeyFor) so short dev secrets
        // signed by @nestjs/jwt do not trip jjwt's 256-bit key length check.
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Matcher m = COMPANY_PATH.matcher(request.getRequestURI());
        if (!m.matches()) {
            chain.doFilter(request, response);
            return;
        }
        String pathCompany = m.group(1);

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            deny(response, 401, "MISSING_TOKEN", "Authorization Bearer token required");
            return;
        }

        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(header.substring(7)).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            deny(response, 401, "INVALID_TOKEN", "Invalid or expired token");
            return;
        }

        String tokenCompany = claims.get("companyCode", String.class);
        if (tokenCompany == null || !tokenCompany.equals(pathCompany)) {
            deny(response, 403, "COMPANY_MISMATCH",
                    "Token company '" + tokenCompany + "' cannot access company '" + pathCompany + "'");
            return;
        }

        chain.doFilter(request, response);
    }

    private void deny(HttpServletResponse response, int status, String code, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("status", code, "detail", detail));
    }
}
