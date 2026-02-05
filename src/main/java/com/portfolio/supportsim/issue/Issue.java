package com.portfolio.supportsim.issue;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(name = "Issue", description = "Represents a support issue with its current status and creation timestamp")
public record Issue(
	@Schema(description = "Unique issue identifier", example = "4") long id,
	@Schema(description = "Short human-readable title of the issue", example = "Unable to login to portal") String title,
	@Schema(description = "Current lifecycle status of the issue. One of: OPEN, INVESTIGATING, RESOLVED, CLOSED", example = "OPEN") String status,
	@Schema(description = "ISO-8601 timestamp when the issue was created", example = "2026-02-05T15:00:00Z") OffsetDateTime createdAt
) {}
