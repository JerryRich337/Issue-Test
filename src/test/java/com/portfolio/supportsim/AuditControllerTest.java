package com.portfolio.supportsim;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
class AuditControllerTest {

  @Autowired MockMvc mvc;

  @Test
  void adminCanReadAuditForIssue() throws Exception {
    // create an issue (admin)
    var location =
        mvc.perform(
                post("/api/issues")
                    .with(httpBasic("admin", "adminpass"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Audit test issue\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    // change status (agent)
    mvc.perform(
            patch(location + "/status")
                .with(httpBasic("agent", "agentpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INVESTIGATING\"}"))
        .andExpect(status().isOk());

    // read audit as admin (all)
    mvc.perform(get(location + "/audit").with(httpBasic("admin", "adminpass")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.items[0].action").exists())
      .andExpect(jsonPath("$.items[0].createdAt", notNullValue()))
      .andExpect(jsonPath("$.total").exists())
      .andExpect(jsonPath("$.limit").exists())
      .andExpect(jsonPath("$.offset").exists());

    // paginated (limit=1)
    mvc.perform(get(location + "/audit").param("limit", "1").with(httpBasic("admin", "adminpass")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.items.length()", org.hamcrest.Matchers.is(1)))
      .andExpect(jsonPath("$.total", org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
      .andExpect(jsonPath("$.limit", org.hamcrest.Matchers.is(1)))
      .andExpect(jsonPath("$.offset", org.hamcrest.Matchers.is(0)));

    // filter by actor (agent)
    mvc.perform(get(location + "/audit").param("actor", "agent").with(httpBasic("admin", "adminpass")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.items.length()", is(1)))
      .andExpect(jsonPath("$.items[0].actor", is("agent")));

    // filter by date range (should include our events)
    var from = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5).toString();
    var to = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5).toString();
    mvc.perform(get(location + "/audit").param("from", from).param("to", to).with(httpBasic("admin", "adminpass")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.items.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
  }
}
