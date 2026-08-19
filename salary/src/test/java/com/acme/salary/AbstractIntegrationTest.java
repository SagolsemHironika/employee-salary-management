package com.acme.salary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

/**
 * Boots the full app against a throwaway Postgres container (Flyway
 * migrations included), rather than the shared local dev database, so
 * integration tests are isolated and reproducible.
 *
 * <p>The container is started once via a static initializer and never
 * explicitly stopped (Testcontainers' Ryuk reaper cleans it up when the JVM
 * exits) - the "singleton container" pattern. All IT subclasses inherit the
 * same static field, so they share one container instead of each paying
 * container-startup cost; letting JUnit's {@code @Testcontainers}/{@code
 * @Container} manage the lifecycle here would stop the container after the
 * first subclass's tests finish, breaking every subclass that runs after it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** Logs in as the auto-bootstrapped default admin (see AdminUserInitializer) and returns a bearer token. */
    protected String adminToken() throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@acme.example\",\"password\":\"ChangeMe123!\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("token").asString();
    }
}
