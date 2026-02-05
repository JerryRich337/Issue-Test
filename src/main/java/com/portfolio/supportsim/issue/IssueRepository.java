package com.portfolio.supportsim.issue;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class IssueRepository {
  private final JdbcTemplate jdbc;

  public IssueRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private static final RowMapper<Issue> ISSUE_ROW_MAPPER =
      (rs, rowNum) ->
          new Issue(
              rs.getLong("id"),
              rs.getString("title"),
              rs.getString("status"),
              toOffsetDateTime(rs.getObject("created_at")));

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

  public List<Issue> findAll() {
    return findAll(null, 50, 0);
  }

  public long countAll(String status) {
    if (status == null) {
      Long count = jdbc.queryForObject("SELECT COUNT(*) FROM issues", Long.class);
      return count == null ? 0 : count;
    }

    Long count = jdbc.queryForObject("SELECT COUNT(*) FROM issues WHERE status = ?", Long.class, status);
    return count == null ? 0 : count;
  }

  public List<Issue> findAll(String status, int limit, int offset) {
    StringBuilder sql =
        new StringBuilder("SELECT id, title, status, created_at FROM issues");
    List<Object> args = new ArrayList<>();

    if (status != null) {
      sql.append(" WHERE status = ?");
      args.add(status);
    }

    sql.append(" ORDER BY id LIMIT ? OFFSET ?");
    args.add(limit);
    args.add(offset);

    return jdbc.query(sql.toString(), ISSUE_ROW_MAPPER, args.toArray());
  }

  public Optional<Issue> findById(long id) {
    try {
      Issue issue =
          jdbc.queryForObject(
              "SELECT id, title, status, created_at FROM issues WHERE id = ?",
              ISSUE_ROW_MAPPER,
              id);
      return Optional.ofNullable(issue);
    } catch (EmptyResultDataAccessException ex) {
      return Optional.empty();
    }
  }

  public Issue create(String title, String status) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  "INSERT INTO issues (title, status) VALUES (?, ?)", new String[] {"id"});
          ps.setString(1, title);
          ps.setString(2, status);
          return ps;
        },
        keyHolder);

    Number id = keyHolder.getKey();
    if (id == null) {
      throw new IllegalStateException("Insert did not return generated id");
    }

    return findById(id.longValue())
        .orElseThrow(() -> new IllegalStateException("Inserted issue not found"));
  }

  public boolean updateStatus(long id, String status) {
    int rows = jdbc.update("UPDATE issues SET status = ? WHERE id = ?", status, id);
    return rows > 0;
  }
}
