package com.portfolio.supportsim.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestIdFilter extends OncePerRequestFilter {
  public static final String HEADER = "X-Request-Id";
  public static final String MDC_KEY = "requestId";

  private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Instant start = Instant.now();

    String requestId = normalize(request.getHeader(HEADER));
    if (requestId == null) {
      requestId = UUID.randomUUID().toString();
    }

    response.setHeader(HEADER, requestId);

    RequestIdContext.set(requestId);
    MDC.put(MDC_KEY, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      response.setHeader(HEADER, requestId);
      long durationMs = Duration.between(start, Instant.now()).toMillis();
      log.info(
          "{} {} -> {} ({}ms) {}={}",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs,
          HEADER,
          requestId);
      MDC.remove(MDC_KEY);
      RequestIdContext.clear();
    }
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    // Avoid log/header abuse.
    if (trimmed.length() > 64) {
      return null;
    }
    if (!trimmed.matches("[A-Za-z0-9._-]+")) {
      return null;
    }
    return trimmed;
  }
}
