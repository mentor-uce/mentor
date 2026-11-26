package com.mentor;

import java.net.ServerSocket;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.socket.client.IO;
import io.socket.client.Socket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"mentor.socket.enabled=true", "mentor.socket.host=127.0.0.1"})
@Import({PostgreSqlTestConfiguration.class, MongoDbTestConfiguration.class,
        RedisTestConfiguration.class, JwtTestConfiguration.class})
class SocketIoHttpTests {
    private static final int PORT = unusedPort();
    @Autowired TestTokenIssuer tokens;

    @DynamicPropertySource
    static void socketProperties(DynamicPropertyRegistry registry) {
        registry.add("mentor.socket.port", () -> PORT);
    }

    @Test void authenticatedSocketIoClientExchangesPingPongAndReconnects() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            var client = client(tokens.token("STUDENT"), "http://localhost:5173", "/socket.io");
            var pong = new CompletableFuture<Object>();
            client.on("pong", args -> pong.complete(args[0]));
            client.on(Socket.EVENT_CONNECT, args -> client.emit("ping", "hello"));
            try {
                client.connect();
                assertEquals("pong", pong.completeOnTimeout(null, 8, TimeUnit.SECONDS).get(9, TimeUnit.SECONDS));
            } finally { client.disconnect(); client.close(); }
        }
    }

    @Test void missingOrInvalidTokensCannotConnect() throws Exception {
        for (String token : new String[]{"", "not-a-jwt", tokens.wrongSignature(),
                tokens.token(Map.of("aud", List.of("other-api"))),
                tokens.token(Map.of("iss", "https://untrusted.invalid")),
                tokens.token(Map.of("exp", Date.from(Instant.now().minusSeconds(120))))}) {
            rejected(client(token, "http://localhost:5173", "/socket.io"));
        }
    }

    @Test void untrustedOriginAndWrongEngineIoPathAreRejected() throws Exception {
        rejected(client(tokens.token("STUDENT"), "https://untrusted.invalid", "/socket.io"));
        rejected(client(tokens.token("STUDENT"), "http://localhost:5173", "/wrong-path"));
    }

    @Test void aConnectedClientIsDisconnectedAtTokenExpiry() throws Exception {
        var client = client(tokens.token(Map.of("exp", Date.from(Instant.now().plusSeconds(8)))),
                "http://localhost:5173", "/socket.io");
        var connected = new CompletableFuture<Boolean>();
        var disconnected = new CompletableFuture<Boolean>();
        client.on(Socket.EVENT_CONNECT, args -> connected.complete(true));
        client.on(Socket.EVENT_DISCONNECT, args -> disconnected.complete(true));
        try {
            client.connect();
            assertTrue(connected.get(5, TimeUnit.SECONDS));
            assertTrue(disconnected.get(12, TimeUnit.SECONDS));
            assertFalse(client.connected());
        } finally { client.disconnect(); client.close(); }
    }

    @Test void urlTokensAreRejectedEvenWithValidAuthPayload() throws Exception {
        var options = new IO.Options();
        options.transports = new String[]{"websocket"};
        options.reconnection = false;
        options.forceNew = true;
        options.timeout = 4000;
        options.query = "token=forbidden-url-token";
        options.extraHeaders = Map.of("Origin", List.of("http://localhost:5173"));
        options.auth = Map.of("token", tokens.token("STUDENT"));
        rejected(IO.socket("http://127.0.0.1:" + PORT + "/mentor", options));
    }

    private Socket client(String token, String origin, String path) throws Exception {
        var options = new IO.Options();
        options.transports = new String[]{"websocket"};
        options.reconnection = false;
        options.forceNew = true;
        options.timeout = 4000;
        options.path = path;
        options.extraHeaders = Map.of("Origin", List.of(origin));
        options.auth = token.isEmpty() ? Map.of() : Map.of("token", token);
        return IO.socket("http://127.0.0.1:" + PORT + "/mentor", options);
    }

    private void rejected(Socket client) throws Exception {
        var error = new CompletableFuture<Boolean>();
        client.on(Socket.EVENT_CONNECT_ERROR, args -> error.complete(true));
        try {
            client.connect();
            assertTrue(error.get(7, TimeUnit.SECONDS));
            assertFalse(client.connected());
        } finally { client.disconnect(); client.close(); }
    }

    private static int unusedPort() {
        try (var socket = new ServerSocket(0)) { return socket.getLocalPort(); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
}