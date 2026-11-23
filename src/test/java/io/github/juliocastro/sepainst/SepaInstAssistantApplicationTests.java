package io.github.juliocastro.sepainst;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Milestone 1 smoke test: the application starts against a real PostgreSQL + pgvector,
 * the vector_store table is created and the health endpoint reports UP.
 * No model is called: nothing is embedded at startup, so no real API key is needed.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        // Dummy keys let the Gemini clients be created; no model is called in these tests.
        properties = {"spring.ai.google.genai.api-key=test-key",
                      "spring.ai.google.genai.embedding.api-key=test-key"})
class SepaInstAssistantApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void vectorStoreTableIsCreated() {
        Integer tables = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'vector_store'",
                Integer.class);
        assertThat(tables).isEqualTo(1);
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health/db")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void askRejectsABlankQuestionWithAProblemDetail() throws Exception {
        HttpResponse<String> response = postAsk("{\"question\": \"   \"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).contains("application/problem+json"));
        assertThat(response.body()).contains("question must not be blank");
    }

    @Test
    void askRejectsMalformedJson() throws Exception {
        HttpResponse<String> response = postAsk("{not json");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> postAsk(String body) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/ask"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
