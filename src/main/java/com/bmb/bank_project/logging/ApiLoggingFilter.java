package com.bmb.bank_project.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(1)
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("API_LOGGER");

    private static final Pattern TPIN_PATTERN = Pattern.compile(
        "(\"(?:tpin|confirmTpin|newTpin)\"\\s*:\\s*\")([^\"]*)(\")",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Skip Swagger / actuator endpoints
        String uri = request.getRequestURI();
        if (uri.contains("/swagger") || uri.contains("/api-docs") || uri.contains("/webjars")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper reqWrapper  = new ContentCachingRequestWrapper(request,400);
        ContentCachingResponseWrapper respWrapper = new ContentCachingResponseWrapper(response);

        String requestId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long startTime   = System.currentTimeMillis();

        try {
            filterChain.doFilter(reqWrapper, respWrapper);
        } finally {
            long duration    = System.currentTimeMillis() - startTime;
            String reqBody   = extractBody(reqWrapper.getContentAsByteArray());
            String respBody  = extractBody(respWrapper.getContentAsByteArray());

            log.info("[{}] REQUEST  | {} {} | Body: {}",
                requestId, request.getMethod(), uri, maskTpin(reqBody));

            log.info("[{}] RESPONSE | {} {} | Status: {} | Duration: {}ms | Body: {}",
                requestId, request.getMethod(), uri,
                response.getStatus(), duration, maskTpin(respBody));

            respWrapper.copyBodyToResponse();
        }
    }

    private String extractBody(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "<empty>";
        return new String(bytes, StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
    }

    private String maskTpin(String body) {
        if (body == null) return null;
        return TPIN_PATTERN.matcher(body).replaceAll("$1****$3");
    }
}
