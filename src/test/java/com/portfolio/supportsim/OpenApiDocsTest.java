package com.portfolio.supportsim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void servesApiDocs() throws Exception {
    String json =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(json).contains("\"/api/issues\"");
    assertThat(json).contains("X-Request-Id");

    JsonNode openApi = objectMapper.readTree(json);

    JsonNode issueListContent =
        openApi.at("/paths/~1api~1issues/get/responses/200/content");
    String issueListSchemaRef = "";
    if (issueListContent.has("application/json")) {
      issueListSchemaRef = issueListContent.at("/application~1json/schema/$ref").asText();
    } else if (issueListContent.has("*/*")) {
      issueListSchemaRef = issueListContent.at("/*~1*/schema/$ref").asText();
    }
    assertThat(issueListSchemaRef).isEqualTo("#/components/schemas/IssuePage");

    // Ensure the controller-provided example for IssuePage is present in the generated docs
    assertThat(json).contains("Unable to login to portal");
    assertThat(json).contains("\"total\":1");

    JsonNode issuePageSchema = openApi.at("/components/schemas/IssuePage");
    assertThat(issuePageSchema.at("/type").asText()).isEqualTo("object");

    JsonNode properties = issuePageSchema.at("/properties");
    assertThat(properties.has("items")).isTrue();
    assertThat(properties.has("total")).isTrue();
    assertThat(properties.has("limit")).isTrue();
    assertThat(properties.has("offset")).isTrue();

    assertThat(properties.at("/items/type").asText()).isEqualTo("array");
    assertThat(properties.at("/items/items/$ref").asText()).isEqualTo("#/components/schemas/Issue");
  }
}
