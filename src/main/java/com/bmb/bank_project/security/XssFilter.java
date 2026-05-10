package com.bmb.bank_project.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet-level filter (runs before Spring Security) that wraps every
 * JSON / form request in an XssRequestWrapper so XSS payloads in the body
 * and URL parameters are stripped before any controller sees them.
 *
 * Order(0) ensures it executes before ApiLoggingFilter(Order=1), so the
 * logged body is already sanitised.
 */
@Component
@Order(0)
public class XssFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String contentType = request.getContentType();
        boolean sanitizable = contentType == null
                || contentType.contains(MediaType.APPLICATION_JSON_VALUE)
                || contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                || contentType.contains(MediaType.TEXT_PLAIN_VALUE);

        if (sanitizable) {
            chain.doFilter(new XssRequestWrapper(request), response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
