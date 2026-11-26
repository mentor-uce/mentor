package com.mentor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class, MongoDbTestConfiguration.class, RedisTestConfiguration.class, JwtTestConfiguration.class})
class SecurityHttpTests {
    @Autowired TestTokenIssuer tokens;
    @Autowired Environment environment;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test void protectedRouteRequiresBearerToken() throws Exception {
        var response = get("/api/v1/test/secured", null);
        assertEquals(401, response.statusCode());
        assertTrue(response.headers().firstValue("WWW-Authenticate").orElse("").contains("Bearer"));
        assertFalse(response.headers().firstValue("Set-Cookie").isPresent());
    }

    @Test void signedAccessTokenAuthenticatesWithoutSession() throws Exception {
        var response = get("/api/v1/test/secured", tokens.token("STUDENT"));
        assertEquals(200, response.statusCode());
        assertEquals("secured", response.body());
        assertFalse(response.headers().firstValue("Set-Cookie").isPresent());
    }

    @Test void invalidTokensAreRejected() throws Exception {
        for (var token : List.of("not-a-jwt", tokens.wrongSignature(),
                tokens.token(Map.of("iss", "https://untrusted.invalid")),
                tokens.token(Map.of("aud", List.of("mentor-web"))),
                tokens.token(Map.of("exp", Date.from(Instant.now().minusSeconds(180)))),
                tokens.token(Map.of("nbf", Date.from(Instant.now().plusSeconds(600)))),
                tokens.token(Map.of("sub", "")))) {
            assertEquals(401, get("/api/v1/test/secured", token).statusCode());
        }
        var noExpiration = new HashMap<String, Object>(); noExpiration.put("exp", null);
        assertEquals(401, get("/api/v1/test/secured", tokens.token(noExpiration)).statusCode());
    }

    @Test void onlyKnownRealmRolesGrantApplicationPermissions() throws Exception {
        assertEquals(403, get("/api/v1/test/admin", tokens.token("STUDENT")).statusCode());
        assertEquals(200, get("/api/v1/test/admin", tokens.token("ADMIN")).statusCode());
        assertEquals(403, get("/api/v1/test/admin", tokens.token("ROLE_ADMIN")).statusCode());
        assertEquals(403, get("/api/v1/test/admin", tokens.token(Map.of("realm_access", "invalid"))).statusCode());
    }

    @Test void managementRoutesStayDeniedEvenWithValidToken() throws Exception {
        assertEquals(403, get("/actuator/env", tokens.token("ADMIN")).statusCode());
        assertEquals(403, get("/actuator/configprops", tokens.token("ADMIN")).statusCode());
    }

    @Test void authenticatedMissingRouteRemainsNotFound() throws Exception {
        assertEquals(404, get("/api/v1/unmapped", tokens.token("STUDENT")).statusCode());
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"
                + environment.getRequiredProperty("local.server.port") + path));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
