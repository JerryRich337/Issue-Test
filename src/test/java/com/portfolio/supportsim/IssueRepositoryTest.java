package com.portfolio.supportsim;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.supportsim.issue.IssueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class IssueRepositoryTest {

  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
      .withDatabaseName("support_sim")
      .withUsername("support")
      .withPassword("support");

  @DynamicPropertySource
  static void datasourceProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  IssueRepository repo;

  @Test
  void findAllReturnsSeededIssues() {
    var issues = repo.findAll();
    assertThat(issues).isNotEmpty();
    assertThat(issues.getFirst().title()).isNotBlank();
  }

  @Test
  void createInsertsAndReturnsIssue() {
    var created = repo.create("Created from test", "OPEN");
    assertThat(created.id()).isPositive();
    assertThat(created.title()).isEqualTo("Created from test");
    assertThat(created.status()).isEqualTo("OPEN");
    assertThat(created.createdAt()).isNotNull();

    var loaded = repo.findById(created.id());
    assertThat(loaded).isPresent();
    assertThat(loaded.get().title()).isEqualTo("Created from test");
  }
}
