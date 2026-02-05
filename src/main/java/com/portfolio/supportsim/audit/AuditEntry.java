package com.portfolio.supportsim.audit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record AuditEntry(
    @Schema(description = "Unique audit record identifier (database primary key)", example = "123") long id,
    @Schema(description = "Identifier of the related issue; matches `Issue.id`", example = "4") Long issueId,
    @Schema(description = "Canonical action name describing what occurred. Examples: `CREATED`, `UPDATED_STATUS`, `COMMENT_ADDED`, `ASSIGNED`.", example = "CREATED") String action,
    @Schema(description = "Actor that performed the action (application username). May be a human user or an automated service account.", example = "admin") String actor,
    @Schema(description = "Request correlation id propagated from the original request (UUID). Useful for tracing across services and logs.", example = "5f50a4b2-fc73-4129-9e71-6e7229b49bbd") String requestId,
    @Schema(description = "Optional free-form details describing the change. This may contain key=value pairs or a short JSON fragment depending on the producer.", example = "title=New issue,status=OPEN") String details,
    @Schema(description = "Timestamp (ISO-8601) when the audit entry was recorded; includes zone/offset.", example = "2026-02-05T15:04:05Z") OffsetDateTime createdAt) {}
