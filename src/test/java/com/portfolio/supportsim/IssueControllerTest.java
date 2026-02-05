package com.portfolio.supportsim;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.everyItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class IssueControllerTest {

  @Autowired MockMvc mvc;
        @Autowired JdbcTemplate jdbc;

        private long auditCount() {
                Long count = jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Long.class);
                return count == null ? 0 : count;
        }

  @Test
  void createReturns201AndCreatedIssue() throws Exception {
                long before = auditCount();
    mvc.perform(
            post("/api/issues")
                .with(httpBasic("admin", "adminpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"New issue from test\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().exists("X-Request-Id"))
        .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/issues/")))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.title", is("New issue from test")))
        .andExpect(jsonPath("$.status", is("OPEN")))
        .andExpect(jsonPath("$.createdAt").exists());

                org.assertj.core.api.Assertions.assertThat(auditCount()).isEqualTo(before + 1);
  }

  @Test
  void echoesClientProvidedRequestId() throws Exception {
    mvc.perform(
            post("/api/issues")
                .with(httpBasic("admin", "adminpass"))
                .header("X-Request-Id", "client-req-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Echo header test\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("X-Request-Id", is("client-req-123")));
  }

  @Test
  void createIsForbiddenForAgentRole() throws Exception {
                long before = auditCount();
    mvc.perform(
            post("/api/issues")
                .with(httpBasic("agent", "agentpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Agent cannot create\"}"))
        .andExpect(status().isForbidden());

                org.assertj.core.api.Assertions.assertThat(auditCount()).isEqualTo(before);
  }

  @Test
  void createRejectsBlankTitle() throws Exception {
    mvc.perform(
            post("/api/issues")
                .with(httpBasic("admin", "adminpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(header().exists("X-Request-Id"))
        .andExpect(jsonPath("$.error", is("validation_error")))
        .andExpect(jsonPath("$.requestId").exists())
        .andExpect(jsonPath("$.details.errors").isArray());
  }

  @Test
  void createRejectsInvalidStatus() throws Exception {
    mvc.perform(
            post("/api/issues")
                .with(httpBasic("admin", "adminpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Has bad status\", \"status\":\"NOPE\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(header().exists("X-Request-Id"))
        .andExpect(jsonPath("$.error", is("validation_error")))
        .andExpect(jsonPath("$.requestId").exists())
        .andExpect(jsonPath("$.details.errors").isArray());
  }

  @Test
  void updateStatusReturns200AndUpdatedIssue() throws Exception {
                long before = auditCount();
    var location =
        mvc.perform(
                post("/api/issues")
                                                                                .with(httpBasic("admin", "adminpass"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Status update test\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");
    org.assertj.core.api.Assertions.assertThat(location).isNotBlank();

    // OPEN -> INVESTIGATING
    mvc.perform(
            patch(location + "/status")
                .with(httpBasic("agent", "agentpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INVESTIGATING\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.status", is("INVESTIGATING")))
        .andExpect(jsonPath("$.createdAt", notNullValue()));

        // One audit entry for create + one for status change.
        org.assertj.core.api.Assertions.assertThat(auditCount()).isEqualTo(before + 2);
  }

  @Test
  void updateStatusRejectsInvalidTransition() throws Exception {
    var location =
        mvc.perform(
                post("/api/issues")
                                                                                .with(httpBasic("admin", "adminpass"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Invalid transition test\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");
    org.assertj.core.api.Assertions.assertThat(location).isNotBlank();

    // OPEN -> CLOSED is allowed, then CLOSED -> OPEN is not.
    mvc.perform(
            patch(location + "/status")
                .with(httpBasic("agent", "agentpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CLOSED\"}"))
        .andExpect(status().isOk());

    mvc.perform(
            patch(location + "/status")
                .with(httpBasic("agent", "agentpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"OPEN\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(header().exists("X-Request-Id"))
        .andExpect(jsonPath("$.error", is("invalid_status_transition")))
        .andExpect(jsonPath("$.requestId").exists())
        .andExpect(jsonPath("$.details.from").exists())
        .andExpect(jsonPath("$.details.to", is("OPEN")));
  }

  @Test
  void updateStatusReturns404WhenMissing() throws Exception {
    mvc.perform(
            patch("/api/issues/999999/status")
                .with(httpBasic("agent", "agentpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CLOSED\"}"))
        .andExpect(status().isNotFound())
        .andExpect(header().exists("X-Request-Id"))
        .andExpect(jsonPath("$.error", is("not_found")))
        .andExpect(jsonPath("$.requestId").exists());
  }

        @Test
        void listCanFilterByStatus() throws Exception {
                        mvc.perform(get("/api/issues").with(httpBasic("agent", "agentpass")).queryParam("status", "OPEN"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.items[*].status", everyItem(is("OPEN"))))
                                        .andExpect(jsonPath("$.total").isNumber())
                                        .andExpect(jsonPath("$.limit", is(50)))
                                        .andExpect(jsonPath("$.offset", is(0)));
        }

        @Test
        void listSupportsLimitAndOffset() throws Exception {
                        mvc.perform(
                                        get("/api/issues")
                                                .with(httpBasic("agent", "agentpass"))
                                                .queryParam("limit", "1")
                                                .queryParam("offset", "0"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.items.length()", is(1)))
                                        .andExpect(jsonPath("$.limit", is(1)))
                                        .andExpect(jsonPath("$.offset", is(0)))
                                        .andExpect(jsonPath("$.total").isNumber());
        }

        @Test
        void listRejectsInvalidLimit() throws Exception {
                mvc.perform(get("/api/issues").with(httpBasic("agent", "agentpass")).queryParam("limit", "0"))
                                .andExpect(status().isBadRequest())
                                .andExpect(header().exists("X-Request-Id"))
                                .andExpect(jsonPath("$.error", is("validation_error")))
                                .andExpect(jsonPath("$.requestId").exists())
                                .andExpect(jsonPath("$.details.errors").isArray());
        }
}
