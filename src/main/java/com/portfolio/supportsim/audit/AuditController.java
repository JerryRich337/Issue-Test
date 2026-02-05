package com.portfolio.supportsim.audit;

import com.portfolio.supportsim.issue.IssueRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import com.portfolio.supportsim.api.ApiError;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/issues")
@Tag(name = "Audit", description = "Audit trail endpoints")
@Validated
public class AuditController {
  private final AuditLogRepository auditRepo;
  private final IssueRepository issues;

  public AuditController(AuditLogRepository auditRepo, IssueRepository issues) {
    this.auditRepo = auditRepo;
    this.issues = issues;
  }

    @Operation(
      summary = "Get audit trail for an issue",
      description = "Returns audit entries for the given issue id",
      responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Paged audit entries for the requested issue. `items` contains the page of results; `total` contains the overall matching count.",
        content = @Content(
          schema = @Schema(implementation = AuditPage.class),
          examples = {@ExampleObject(value = "{\"items\":[{\"id\":1,\"issueId\":4,\"action\":\"CREATED\",\"actor\":\"admin\",\"requestId\":\"5c48c61c-8988-4dd3-8a5a-dcf4f9dff53b\",\"details\":null,\"createdAt\":\"2026-02-05T15:00:00Z\"}],\"total\":1,\"limit\":50,\"offset\":0}")}
        )),
      @ApiResponse(
        responseCode = "400",
        description = "Invalid query parameters (e.g. `limit`, `offset`, or malformed date-time)",
        content = @Content(
          schema = @Schema(implementation = ApiError.class),
          examples = {@ExampleObject(value = "{\"error\":\"Bad Request\",\"message\":\"Invalid limit parameter\",\"requestId\":\"abcd-1234\",\"details\":null}")}
        )),
      @ApiResponse(
        responseCode = "401",
        description = "Authentication required",
        content = @Content(
          schema = @Schema(implementation = ApiError.class),
          examples = {@ExampleObject(value = "{\"error\":\"Unauthorized\",\"message\":\"Authentication required\",\"requestId\":\"abcd-1234\",\"details\":null}")}
        )),
      @ApiResponse(
        responseCode = "403",
        description = "Insufficient privileges to access this resource",
        content = @Content(
          schema = @Schema(implementation = ApiError.class),
          examples = {@ExampleObject(value = "{\"error\":\"Forbidden\",\"message\":\"Insufficient privileges\",\"requestId\":\"abcd-1234\",\"details\":null}")}
        )),
      @ApiResponse(
        responseCode = "404",
        description = "Issue not found",
        content = @Content(
          schema = @Schema(implementation = ApiError.class),
          examples = {@ExampleObject(value = "{\"error\":\"Not Found\",\"message\":\"Issue not found\",\"requestId\":\"abcd-1234\",\"details\":null}")}
        ))
      })
  @GetMapping("/{id}/audit")
      public ResponseEntity<AuditPage> audit(
        @PathVariable long id,
        @Parameter(
          description = "Maximum number of items to return",
          example = "50",
          required = false,
          schema = @Schema(type = "integer", minimum = "1", maximum = "200", defaultValue = "50"))
        @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
        @Parameter(
          description = "Offset into the result set",
          example = "0",
          required = false,
          schema = @Schema(type = "integer", minimum = "0", defaultValue = "0"))
        @RequestParam(defaultValue = "0") @Min(0) int offset,
        @Parameter(description = "Filter by actor (username)", example = "agent", required = false)
        @RequestParam(required = false) String actor,
        @Parameter(description = "Start of date-time range (inclusive) in ISO-8601 format",
          example = "2026-02-05T10:00:00Z",
          required = false)
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
        @Parameter(description = "End of date-time range (inclusive) in ISO-8601 format",
          example = "2026-02-05T12:00:00Z",
          required = false)
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
    var exists = issues.findById(id).isPresent();
    if (!exists) {
      return ResponseEntity.notFound().build();
    }
      var items = auditRepo.findByIssueId(id, limit, offset, actor, from, to);
      var total = auditRepo.countByIssueId(id, actor, from, to);
      return ResponseEntity.ok(new AuditPage(items, total, limit, offset));
  }
}
