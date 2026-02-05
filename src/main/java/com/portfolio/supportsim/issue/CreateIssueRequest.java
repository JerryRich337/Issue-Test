package com.portfolio.supportsim.issue;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateIssueRequest", description = "Request payload to create a new issue. `status` is optional; defaults to OPEN.")
public record CreateIssueRequest(
    @Schema(description = "Short title describing the issue (required)", example = "Cannot access account") @NotBlank @Size(max = 200) String title,
    @Schema(description = "Initial status for the issue (optional). If omitted, defaults to OPEN", example = "OPEN") @Pattern(regexp = "(?i)OPEN|INVESTIGATING|RESOLVED|CLOSED", message = "must be one of OPEN, INVESTIGATING, RESOLVED, CLOSED")
        String status) {}
