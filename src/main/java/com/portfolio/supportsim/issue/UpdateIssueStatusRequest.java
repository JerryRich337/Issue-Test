package com.portfolio.supportsim.issue;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "UpdateIssueStatusRequest", description = "Payload to update an issue's status. `status` must be a valid lifecycle value.")
public record UpdateIssueStatusRequest(
    @Schema(description = "Target status for the issue. Allowed values: OPEN, INVESTIGATING, RESOLVED, CLOSED", example = "RESOLVED") @NotBlank
        @Pattern(
            regexp = "(?i)OPEN|INVESTIGATING|RESOLVED|CLOSED",
            message = "must be one of OPEN, INVESTIGATING, RESOLVED, CLOSED")
        String status) {}
