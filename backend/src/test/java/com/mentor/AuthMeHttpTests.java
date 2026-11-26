package com.mentor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class, MongoDbTestConfiguration.class, RedisTestConfiguration.class, JwtTestConfiguration.class})
class AuthMeHttpTests {
    @Autowired TestTokenIssuer tokens;
    @Autowired Environment environment;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void anonymousMeIsUnauthorized() throws Exception {
        assertEquals(401, get(null).statusCode());
    }

    @Test void meReturnsOnlyAuthenticatedIdentityAndKnownRoles() throws Exception {
        var token = tokens.token(Map.of("realm_access", Map.of("roles", List.of("TUTOR", "STUDENT", "offline_access", "TUTOR")),
                "private_claim", "must-not-leak", "resource_access", Map.of("other", Map.of("roles", List.of("ADMIN")))));
        var response = get(token);
        assertEquals(200, response.statusCode());
        var body = mapper.readTree(response.body());
        assertEquals(4, body.size());
        assertEquals("student-id", body.get("id").asString());
        assertEquals("student.local", body.get("username").asString());
        assertEquals("student@example.invalid", body.get("email").asString());
        assertEquals(mapper.readTree("[\"STUDENT\",\"TUTOR\"]"), body.get("roles"));
        assertFalse(response.body().contains("must-not-leak"));
        assertFalse(response.body().contains(token));
        assertFalse(response.headers().firstValue("Set-Cookie").isPresent());
        assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
    }

    @Test void malformedRolesNeverGrantPermissions() throws Exception {
        var response = get(tokens.token(Map.of("realm_access", Map.of("roles", "ADMIN"))));
        assertEquals(200, response.statusCode());
        assertEquals(mapper.readTree("[]"), mapper.readTree(response.body()).get("roles"));
    }

    @Test void malformedProfileClaimsAreNotStringified() throws Exception {
        var response = get(tokens.token(Map.of("email", Map.of("private", "nested-secret"),
                "preferred_username", List.of("nested-secret"))));
        assertEquals(200, response.statusCode());
        var body = mapper.readTree(response.body());
        assertTrue(body.get("username").isNull());
        assertTrue(body.get("email").isNull());
        assertFalse(response.body().contains("nested-secret"));
    }

    private HttpResponse<String> get(String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"
                + environment.getRequiredProperty("local.server.port") + "/api/v1/auth/me"));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
