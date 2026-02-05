package com.portfolio.supportsim.audit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "AuditPage", description = "Paged container for audit entries with total matching count and paging metadata")
public class AuditPage {
  @Schema(description = "The list of audit entries returned for this page. Contains at most `limit` elements.")
  private final List<AuditEntry> items;

  @Schema(description = "Total number of audit entries that match the request filters across all pages. Use this to compute total pages.")
  private final long total;

  @Schema(description = "The requested page size (maximum number of items returned). May be smaller if end of result set is reached.")
  private final int limit;

  @Schema(description = "Zero-based offset into the full result set indicating the position of the first element in `items`.")
  private final int offset;

  public AuditPage(List<AuditEntry> items, long total) {
    this.items = items;
    this.total = total;
    this.limit = items == null ? 0 : items.size();
    this.offset = 0;
  }

  public AuditPage(List<AuditEntry> items, long total, int limit, int offset) {
    this.items = items;
    this.total = total;
    this.limit = limit;
    this.offset = offset;
  }

  public List<AuditEntry> getItems() {
    return items;
  }

  public long getTotal() {
    return total;
  }

  public int getLimit() {
    return limit;
  }

  public int getOffset() {
    return offset;
  }
}
