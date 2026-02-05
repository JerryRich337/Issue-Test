package com.portfolio.supportsim.issue;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.portfolio.supportsim.audit.AuditLogger;
import com.portfolio.supportsim.api.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/issues")
@Validated
public class IssueController {
  private final IssueRepository repo;
  private final AuditLogger audit;

  private static final Map<String, Set<String>> ALLOWED_STATUS_TRANSITIONS =
      Map.of(
          "OPEN", Set.of("OPEN", "INVESTIGATING", "RESOLVED", "CLOSED"),
          "INVESTIGATING", Set.of("INVESTIGATING", "RESOLVED", "CLOSED"),
          "RESOLVED", Set.of("RESOLVED", "CLOSED"),
          "CLOSED", Set.of("CLOSED"));

  public IssueController(IssueRepository repo, AuditLogger audit) {
    this.repo = repo;
    this.audit = audit;
  }

  @GetMapping
    @Operation(
      summary = "List issues",
      description = "Return a paginated list of issues. Filter by status if provided.",
      responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Paged issues for the requested filters",
        content = @Content(
            schema = @Schema(implementation = IssuePage.class),
            examples = {
              @ExampleObject(
                  value = "{\"items\":[{\"id\":4,\"title\":\"Unable to login to portal\",\"status\":\"OPEN\",\"createdAt\":\"2026-02-05T15:00:00Z\"}],\"total\":1,\"limit\":50,\"offset\":0}")
            })),
      @ApiResponse(
        responseCode = "400",
        description = "Invalid query parameters (e.g. `limit`, `offset`, or `status`)",
        content =
          @Content(
            schema =
              @Schema(implementation = com.portfolio.supportsim.api.ApiError.class)))
      })
  public IssuePage list(
      @RequestParam(required = false)
          @Pattern(
              regexp = "(?i)OPEN|INVESTIGATING|RESOLVED|CLOSED",
              message = "must be one of OPEN, INVESTIGATING, RESOLVED, CLOSED")
        @Parameter(description = "Filter by issue status (case-insensitive)", example = "OPEN") String status,
      @Parameter(description = "Maximum number of items to return", example = "50") @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
      @Parameter(description = "Zero-based offset into results", example = "0") @RequestParam(defaultValue = "0") @Min(0) int offset) {
    String normalizedStatus = status == null ? null : status.trim().toUpperCase();
    var items = repo.findAll(normalizedStatus, limit, offset);
    var total = repo.countAll(normalizedStatus);
    return new IssuePage(items, total, limit, offset);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get single issue",
      description = "Retrieve a single issue by id",
      responses = {
        @ApiResponse(responseCode = "200", description = "Issue found", content = @Content(schema = @Schema(implementation = Issue.class))),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content(schema = @Schema(implementation = com.portfolio.supportsim.api.ApiError.class)))
      })
  public ResponseEntity<?> get(@PathVariable long id) {
    return repo
        .findById(id)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(
            () ->
                ResponseEntity.status(404)
                    .body(ApiError.of("not_found", "Issue not found", Map.of("id", id))));
  }

  @PostMapping
  @Operation(summary = "Create issue", description = "Create a new issue. Returns `201 Created` with `Location` header.")
  public ResponseEntity<Issue> create(@Valid @RequestBody CreateIssueRequest request) {
    String title = request.title().trim();
    String status = request.status() == null ? "OPEN" : request.status().trim().toUpperCase();

    Issue created = repo.create(title, status);
    audit.issueCreated(created.id(), created.title(), created.status());
    return ResponseEntity.created(URI.create("/api/issues/" + created.id())).body(created);
  }

  @PatchMapping("/{id}/status")
  @Operation(
      summary = "Update issue status",
      description = "Update the lifecycle status of an issue. Validates allowed transitions.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Status updated", content = @Content(schema = @Schema(implementation = Issue.class))),
        @ApiResponse(responseCode = "400", description = "Invalid transition or payload", content = @Content(schema = @Schema(implementation = com.portfolio.supportsim.api.ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content(schema = @Schema(implementation = com.portfolio.supportsim.api.ApiError.class)))
      })
  public ResponseEntity<?> updateStatus(
      @PathVariable long id, @Valid @RequestBody UpdateIssueStatusRequest request) {
    var existingOpt = repo.findById(id);
    if (existingOpt.isEmpty()) {
      return ResponseEntity.status(404).body(ApiError.of("not_found", "Issue not found", Map.of("id", id)));
    }

    Issue existing = existingOpt.get();
    String from = existing.status() == null ? "OPEN" : existing.status().trim().toUpperCase();
    String to = request.status().trim().toUpperCase();

    var allowed = ALLOWED_STATUS_TRANSITIONS.get(from);
    if (allowed == null || !allowed.contains(to)) {
      return ResponseEntity.badRequest()
        .body(
          ApiError.of(
            "invalid_status_transition",
            "Status change is not allowed",
            Map.of(
              "from", from,
              "to", to,
              "allowed", allowed == null ? List.of() : allowed)));
    }

    repo.updateStatus(id, to);
    audit.issueStatusChanged(id, from, to);
    return repo
      .findById(id)
      .<ResponseEntity<?>>map(ResponseEntity::ok)
      .orElseGet(
        () ->
          ResponseEntity.status(404)
            .body(ApiError.of("not_found", "Issue not found", Map.of("id", id))));
  }
}
