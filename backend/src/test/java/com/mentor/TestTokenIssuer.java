package com.mentor;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

/** Test-only issuer: real RSA signatures and an HTTP JWKS boundary. */
class TestTokenIssuer implements AutoCloseable {
    private final RSAKey key = new RSAKeyGenerator(2048).keyID("mentor-test-key").generate();
    private final HttpServer server;

    TestTokenIssuer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
    }

    String issuer() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/realms/mentor"; }
    String jwksUri() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks"; }
    String token(String role) throws Exception { return token(Map.of("realm_access", Map.of("roles", List.of(role)))); }

    String token(Map<String, Object> overrides) throws Exception { return signed(overrides, key); }

    String wrongSignature() throws Exception {
        return signed(Map.of(), new RSAKeyGenerator(2048).keyID("mentor-test-key").generate());
    }

    private String signed(Map<String, Object> overrides, RSAKey signingKey) throws Exception {
        Map<String, Object> values = new HashMap<>();
        values.put("iss", issuer()); values.put("aud", List.of("mentor-api"));
        values.put("sub", "student-id"); values.put("preferred_username", "student.local");
        values.put("email", "student@example.invalid");
        values.put("iat", Date.from(Instant.now()));
        values.put("exp", Date.from(Instant.now().plusSeconds(300)));
        values.put("realm_access", Map.of("roles", List.of("STUDENT")));
        values.putAll(overrides);
        var builder = new JWTClaimsSet.Builder();
        values.forEach(builder::claim);
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(signingKey.getKeyID()).type(JOSEObjectType.JWT).build(), builder.build());
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }

    @Override public void close() { server.stop(0); }
}
