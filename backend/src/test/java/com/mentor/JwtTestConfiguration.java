package com.mentor;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@TestConfiguration(proxyBeanMethods = false)
class JwtTestConfiguration {
    @Bean(destroyMethod = "close")
    TestTokenIssuer testTokenIssuer() throws Exception {
        return new TestTokenIssuer();
    }

    @Bean
    DynamicPropertyRegistrar jwtProperties(TestTokenIssuer tokens) {
        return registry -> {
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", tokens::issuer);
            registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", tokens::jwksUri);
        };
    }

@RestController
    @TestComponent
    static class SecurityProbe {
        @GetMapping("/api/v1/test/secured")
        public String secured() { return "secured"; }

        @GetMapping("/api/v1/test/admin")
        @PreAuthorize("hasRole('ADMIN')")
        public String admin() { return "admin"; }
    }
}
