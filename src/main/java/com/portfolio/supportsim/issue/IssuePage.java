package com.portfolio.supportsim.issue;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
    name = "IssuePage",
    description = "Paged container for issues with total matching count and paging metadata")
public class IssuePage {
  @Schema(description = "The issues returned for this page. Contains at most `limit` elements.")
  private final List<Issue> items;

  @Schema(
      description =
          "Total number of issues matching the request filters across all pages. Use this to compute total pages.")
  private final long total;

  @Schema(
      description =
          "The requested page size (maximum number of items returned). May be smaller if end of result set is reached.")
  private final int limit;

  @Schema(
      description =
          "Zero-based offset into the full result set indicating the position of the first element in `items`.")
  private final int offset;

  public IssuePage(List<Issue> items, long total, int limit, int offset) {
    this.items = items;
    this.total = total;
    this.limit = limit;
    this.offset = offset;
  }

  public List<Issue> getItems() {
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
