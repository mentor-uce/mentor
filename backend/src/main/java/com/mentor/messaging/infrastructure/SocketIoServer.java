package com.mentor.messaging.infrastructure;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.corundumstudio.socketio.AuthTokenResult;
import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/** Authenticated connectivity scaffold. Business rooms and chat are separate use cases. */
@Component
@ConditionalOnProperty(name = "mentor.socket.enabled", havingValue = "true")
public class SocketIoServer implements SmartLifecycle {
    private static final String JWT_KEY = "mentor.jwt";
    private final SocketIOServer server;
    private volatile boolean running;
    private ScheduledExecutorService expiryChecks;

    public SocketIoServer(Environment environment, JwtDecoder decoder) {
        Set<String> origins = Arrays.stream(environment.getRequiredProperty("mentor.cors.allowed-origins").split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toUnmodifiableSet());
        if (origins.stream().anyMatch(value -> value.contains("*"))) {
            throw new IllegalArgumentException("Socket.IO requires explicit allowed origins");
        }
        var config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(environment.getProperty("mentor.socket.host", "127.0.0.1"));
        config.setPort(environment.getProperty("mentor.socket.port", Integer.class, 9092));
        config.setContext("/socket.io");
        config.setTransports(Transport.WEBSOCKET);
        config.setBossThreads(1);
        config.setWorkerThreads(2);
        config.setRandomSession(true);
        config.setMaxFramePayloadLength(4096);
        config.setMaxHttpContentLength(4096);
        config.setAuthorizationListener(data -> {
            String origin = data.getHttpHeaders().get("Origin");
            boolean allowed = (origin == null || origins.contains(origin))
                    && !data.getUrlParams().containsKey("token")
                    && !data.getUrlParams().containsKey("access_token");
            return new AuthorizationResult(allowed);
        });
        server = new SocketIOServer(config);
        server.getNamespace("").addAuthTokenListener((data, client) -> unauthorized());
        server.getNamespace("").addConnectListener(SocketIOClient::disconnect);
        var namespace = server.addNamespace("/mentor");
        namespace.addAuthTokenListener((data, client) -> {
            if (!(data instanceof Map<?, ?> auth) || !(auth.get("token") instanceof String token)
                    || token.isBlank() || token.length() > 8192) {
                return unauthorized();
            }
            try {
                Jwt jwt = decoder.decode(token);
                if (jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now())) return unauthorized();
                client.set(JWT_KEY, jwt);
                return AuthTokenResult.AuthTokenResultSuccess;
            } catch (JwtException | IllegalArgumentException error) {
                return unauthorized();
            }
        });
        namespace.addConnectListener(client -> { if (!authenticated(client)) client.disconnect(); });
        namespace.addEventListener("ping", String.class, (client, payload, ack) -> {
            if (!authenticated(client)) { client.disconnect(); return; }
            if (payload != null && payload.length() <= 256) client.sendEvent("pong", "pong");
        });
    }

    private static AuthTokenResult unauthorized() {
        return new AuthTokenResult(false, Map.of("message", "Unauthorized"));
    }

    private static boolean authenticated(SocketIOClient client) {
        Jwt jwt = client.get(JWT_KEY);
        return jwt != null && jwt.getExpiresAt() != null && jwt.getExpiresAt().isAfter(Instant.now());
    }

    @Override public synchronized void start() {
        if (running) return;
        try { server.start(); }
        catch (RuntimeException error) { server.stop(); throw error; }
        expiryChecks = Executors.newSingleThreadScheduledExecutor(task -> {
            var thread = new Thread(task, "mentor-socket-expiry");
            thread.setDaemon(true);
            return thread;
        });
        expiryChecks.scheduleAtFixedRate(() -> server.getNamespace("/mentor").getAllClients().forEach(client -> {
            if (!authenticated(client)) client.disconnect();
        }), 1, 1, TimeUnit.SECONDS);
        running = true;
    }

    @Override public synchronized void stop() {
        if (!running) return;
        running = false;
        expiryChecks.shutdownNow();
        server.stop();
    }

    @Override public boolean isRunning() { return running; }
}