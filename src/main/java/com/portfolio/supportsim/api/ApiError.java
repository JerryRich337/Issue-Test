package com.portfolio.supportsim.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(name = "ApiError", description = "Standardized error response returned when requests fail validation or authorization")
public record ApiError(
    @Schema(description = "Short machine-readable error code", example = "bad_request") String error,
    @Schema(description = "Human-friendly error message", example = "Invalid request parameters") String message,
    @Schema(description = "Request correlation id if available (echoed from incoming request)", example = "abcd-1234") String requestId,
    @Schema(description = "Optional structured details to help clients diagnose the problem", example = "{}") Map<String, Object> details) {
  public static ApiError of(String error, String message) {
    return new ApiError(error, message, RequestIdContext.get(), Map.of());
  }

  public static ApiError of(String error, String message, Map<String, Object> details) {
    return new ApiError(
        error, message, RequestIdContext.get(), details == null ? Map.of() : Map.copyOf(details));
  }
}
