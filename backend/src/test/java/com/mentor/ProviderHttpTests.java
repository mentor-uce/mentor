package com.mentor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import com.mentor.video.domain.VideoProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class, MongoDbTestConfiguration.class,
        RedisTestConfiguration.class, JwtTestConfiguration.class})
class ProviderHttpTests {
    @Autowired TestTokenIssuer tokens;
    @Autowired Environment environment;
    @Autowired ApplicationContext context;

    @Test void paymentStatusRequiresAuthentication() throws Exception {
        assertEquals(401, get(null).statusCode());
    }

    @Test void paymentStatusExplicitlyReportsSafeSimulation() throws Exception {
        var response = get(tokens.token("STUDENT"));
        assertEquals(200, response.statusCode());
        var body = new ObjectMapper().readTree(response.body());
        assertEquals(3, body.size());
        assertEquals("SIMULATED", body.get("provider").asString());
        assertTrue(body.get("testMode").asBoolean());
        assertFalse(body.get("chargesEnabled").asBoolean());
        assertFalse(response.body().contains("secret"));
        assertFalse(response.body().contains("checkoutUrl"));
        assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
    }

    @Test void dummyVideoRoomIsExplicitlyNotJoinable() {
        var providers = context.getBeansOfType(VideoProvider.class);
        assertEquals(1, providers.size(), "A configured dummy VideoProvider is required");
        var provider = providers.values().iterator().next();
        var booking = UUID.randomUUID();
        var room = provider.createRoom(booking);
        assertEquals("DUMMY", room.provider());
        assertEquals("dummy-" + booking, room.roomId());
        assertTrue(room.testMode());
        assertFalse(room.joinable());
        assertThrows(IllegalArgumentException.class, () -> provider.createRoom(null));
    }

    private HttpResponse<String> get(String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"
                + environment.getRequiredProperty("local.server.port") + "/api/v1/payments/status"));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return HttpClient.newHttpClient().send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}