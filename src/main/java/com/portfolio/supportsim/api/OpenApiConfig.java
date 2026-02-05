package com.portfolio.supportsim.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI supportSimulatorOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Support Simulator API")
                .version("0.1.0")
                .description("Issue API for a support simulator service"));
  }

  @Bean
  public OpenApiCustomizer requestIdHeaderCustomiser() {
    return openApi -> {
      if (openApi.getPaths() == null) {
        return;
      }

      Parameter requestIdHeader =
          new Parameter()
              .in("header")
              .name(RequestIdFilter.HEADER)
              .required(false)
              .description("Optional request correlation id (echoed back in responses)")
              .schema(new StringSchema().example("client-req-123"));

      openApi
          .getPaths()
          .values()
          .forEach(
              pathItem ->
                  pathItem
                      .readOperations()
                      .forEach(
                          operation -> {
                            if (operation.getParameters() == null
                                || operation.getParameters().stream()
                                    .noneMatch(
                                        p ->
                                            RequestIdFilter.HEADER.equalsIgnoreCase(p.getName())
                                                && "header".equalsIgnoreCase(p.getIn()))) {
                              operation.addParametersItem(requestIdHeader);
                            }
                          }));
    };
  }
}
