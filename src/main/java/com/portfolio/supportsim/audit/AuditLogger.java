package com.portfolio.supportsim.audit;

import com.portfolio.supportsim.api.RequestIdContext;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditLogger {
  private final AuditLogRepository repo;

  public AuditLogger(AuditLogRepository repo) {
    this.repo = repo;
  }

  public void issueCreated(long issueId, String title, String status) {
    repo.insert(
        issueId,
        "issue_created",
        currentActor(),
        RequestIdContext.get(),
        "title=" + safe(title) + ",status=" + safe(status));
  }

  public void issueStatusChanged(long issueId, String from, String to) {
    repo.insert(
        issueId,
        "issue_status_changed",
        currentActor(),
        RequestIdContext.get(),
        "from=" + safe(from) + ",to=" + safe(to));
  }

  private static String currentActor() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return null;
    }
    Object principal = auth.getPrincipal();
    if (principal == null) {
      return null;
    }
    if (principal instanceof String s) {
      return Objects.equals(s, "anonymousUser") ? null : s;
    }
    return auth.getName();
  }

  private static String safe(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\n", " ").replace("\r", " ").trim();
  }
}
