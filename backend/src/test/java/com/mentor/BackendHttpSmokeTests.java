package com.mentor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "mentor.cors.allowed-origins=http://localhost:5173,http://127.0.0.1:5173")
@Import({PostgreSqlTestConfiguration.class, MongoDbTestConfiguration.class})
class BackendHttpSmokeTests {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @Autowired
    private Environment environment;

    @Test
    void helloReturnsThePublishedJsonContract() throws Exception {
        var response = send(request("/api/v1/hello").GET().build());
        assertEquals(200, response.statusCode());
        assertEquals("{\"message\":\"Mentor backend is running\"}", response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
    }

    @Test
    void healthReturnsUpWithoutInternalDetails() throws Exception {
        var response = send(request("/actuator/health").GET().build());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().matches("(?s).*\"status\"\\s*:\\s*\"UP\".*"));
        assertFalse(response.body().contains("\"components\""));
        assertFalse(response.body().contains("\"details\""));
    }

    @Test
    void sensitiveActuatorEndpointsAreNotExposed() throws Exception {
        assertEquals(404, send(request("/actuator/env").GET().build()).statusCode());
        assertEquals(404, send(request("/actuator/configprops").GET().build()).statusCode());
    }

    @Test
    void openApiDocumentsHelloAndApiMetadata() throws Exception {
        var response = send(request("/v3/api-docs").GET().build());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"/api/v1/hello\""));
        assertTrue(response.body().contains("\"Mentor API\""));
        assertTrue(response.body().contains("\"v1\""));
    }

    @Test
    void swaggerUiIsServedLocally() throws Exception {
        var response = send(request("/swagger-ui/index.html").GET().build());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Swagger UI"));
    }

    @Test
    void configuredOriginsCanReadTheApiWithoutCookieCredentials() throws Exception {
        for (var origin : new String[] { "http://localhost:5173", "http://127.0.0.1:5173" }) {
            var response = send(request("/api/v1/hello").header("Origin", origin).GET().build());
            assertEquals(200, response.statusCode());
            assertEquals(origin, response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
            assertFalse(response.headers().firstValue("Access-Control-Allow-Credentials").isPresent());
        }
    }

    @Test
    void unknownOriginIsRejected() throws Exception {
        var response = send(request("/api/v1/hello").header("Origin", "https://untrusted.invalid")
                .GET().build());
        assertEquals(403, response.statusCode());
        assertFalse(response.headers().firstValue("Access-Control-Allow-Origin").isPresent());
    }

    @Test
    void preflightAllowsTheApiHeadersForAnApprovedOrigin() throws Exception {
        var response = send(request("/api/v1/hello")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization,content-type,idempotency-key")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build());
        assertEquals(200, response.statusCode());
        assertEquals("http://localhost:5173",
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        var allowed = response.headers().firstValue("Access-Control-Allow-Headers").orElse("").toLowerCase();
        assertTrue(allowed.contains("authorization"));
        assertTrue(allowed.contains("content-type"));
        assertTrue(allowed.contains("idempotency-key"));
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:"
                + environment.getRequiredProperty("local.server.port") + path))
                .timeout(Duration.ofSeconds(20));
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
