package com.bmb.bank_project.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Sanitizes all incoming request data (URL parameters and the request body)
 * to defend against XSS payloads.  HTML tags, script blocks, javascript:
 * protocol references, and inline event handlers are stripped.
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] sanitizedBody;

    public XssRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        byte[] raw = request.getInputStream().readAllBytes();
        String body = new String(raw, StandardCharsets.UTF_8);
        this.sanitizedBody = sanitize(body).getBytes(StandardCharsets.UTF_8);
    }

    // InputStream / Reader (used for @RequestBody parsing)

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bis = new ByteArrayInputStream(sanitizedBody);
        return new ServletInputStream() {
            @Override public int read()                              { return bis.read(); }
            @Override public boolean isFinished()                   { return bis.available() == 0; }
            @Override public boolean isReady()                      { return true; }
            @Override public void setReadListener(ReadListener rl)  {}
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(
                new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    // URL / form parameters

    @Override
    public String getParameter(String name) {
        return sanitize(super.getParameter(name));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) return null;
        return Arrays.stream(values).map(this::sanitize).toArray(String[]::new);
    }

    // Core sanitiser

    private String sanitize(String input) {
        if (input == null) return null;
        return input
                // Remove entire <script>…</script> blocks (multi-line)
                .replaceAll("(?is)<script[^>]*>.*?</script>", "")
                // Strip remaining HTML/XML tags
                .replaceAll("(?i)<[^>]+>", "")
                // Remove javascript: protocol (allow spaces between chars)
                .replaceAll("(?i)j\\s*a\\s*v\\s*a\\s*s\\s*c\\s*r\\s*i\\s*p\\s*t\\s*:", "")
                // Remove inline event handlers  (onclick=, onload=, etc.)
                .replaceAll("(?i)on\\w+\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|\\S+)", "")
                // Remove CSS expression() used for IE XSS
                .replaceAll("(?i)expression\\s*\\(", "");
    }
}
