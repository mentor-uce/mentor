package com.mentor.auth.presentation;

import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        var roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(role -> role.startsWith("ROLE_")).map(role -> role.substring(5)).sorted().toList();
        return new MeResponse(jwt.getSubject(), profileClaim(jwt, "preferred_username"),
                profileClaim(jwt, "email"), roles);
    }

    private String profileClaim(Jwt jwt, String name) {
        Object value = jwt.getClaims().get(name);
        return value instanceof String text ? text : null;
    }

    public record MeResponse(String id, String username, String email, List<String> roles) { }
}
