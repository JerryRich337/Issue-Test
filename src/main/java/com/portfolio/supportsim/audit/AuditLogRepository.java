package com.portfolio.supportsim.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;


@Repository
public class AuditLogRepository {
  private final JdbcTemplate jdbc;

  public AuditLogRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void insert(Long issueId, String action, String actor, String requestId, String details) {
    jdbc.update(
        "INSERT INTO audit_log (issue_id, action, actor, request_id, details) VALUES (?, ?, ?, ?, ?)",
        issueId,
        action,
        actor,
        requestId,
        details);
  }

  public List<AuditEntry> findByIssueId(long issueId) {
    return findByIssueId(issueId, 50, 0);
  }

  public List<AuditEntry> findByIssueId(long issueId, int limit, int offset) {
    return jdbc.query(
        "SELECT id, issue_id, action, actor, request_id, details, created_at FROM audit_log WHERE issue_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?",
        (rs, rowNum) ->
            new AuditEntry(
                rs.getLong("id"),
                rs.getObject("issue_id") == null ? null : rs.getLong("issue_id"),
                rs.getString("action"),
                rs.getString("actor"),
                rs.getString("request_id"),
                rs.getString("details"),
                toOffsetDateTime(rs.getObject("created_at"))),
        issueId,
        limit,
        offset);
  }

  public List<AuditEntry> findByIssueId(long issueId, int limit, int offset, String actor, OffsetDateTime from, OffsetDateTime to) {
    var sql = new StringBuilder();
    sql.append("SELECT id, issue_id, action, actor, request_id, details, created_at FROM audit_log WHERE issue_id = ?");
    var params = new java.util.ArrayList<Object>();
    params.add(issueId);

    if (actor != null && !actor.isBlank()) {
      sql.append(" AND actor = ?");
      params.add(actor);
    }
    if (from != null) {
      sql.append(" AND created_at >= ?");
      params.add(Timestamp.from(from.toInstant()));
    }
    if (to != null) {
      sql.append(" AND created_at <= ?");
      params.add(Timestamp.from(to.toInstant()));
    }

    sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
    params.add(limit);
    params.add(offset);

    return jdbc.query(
        sql.toString(),
        (rs, rowNum) ->
            new AuditEntry(
                rs.getLong("id"),
                rs.getObject("issue_id") == null ? null : rs.getLong("issue_id"),
                rs.getString("action"),
                rs.getString("actor"),
                rs.getString("request_id"),
                rs.getString("details"),
                toOffsetDateTime(rs.getObject("created_at"))),
        params.toArray());
  }

  public long countByIssueId(long issueId, String actor, OffsetDateTime from, OffsetDateTime to) {
    var sql = new StringBuilder();
    sql.append("SELECT COUNT(*) FROM audit_log WHERE issue_id = ?");
    var params = new java.util.ArrayList<Object>();
    params.add(issueId);

    if (actor != null && !actor.isBlank()) {
      sql.append(" AND actor = ?");
      params.add(actor);
    }
    if (from != null) {
      sql.append(" AND created_at >= ?");
      params.add(Timestamp.from(from.toInstant()));
    }
    if (to != null) {
      sql.append(" AND created_at <= ?");
      params.add(Timestamp.from(to.toInstant()));
    }

    return jdbc.queryForObject(sql.toString(), params.toArray(), Long.class);
  }

  private static OffsetDateTime toOffsetDateTime(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof OffsetDateTime offsetDateTime) {
      return offsetDateTime;
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime.atOffset(ZoneOffset.UTC);
    }
    throw new IllegalStateException("Unsupported created_at type: " + value.getClass().getName());
  }
}
