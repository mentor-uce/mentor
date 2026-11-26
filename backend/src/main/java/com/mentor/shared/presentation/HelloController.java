package com.mentor.shared.presentation;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @Operation(summary = "Check that the Mentor API is responding")
    @GetMapping("/api/v1/hello")
    public HelloResponse hello() {
        return new HelloResponse("Mentor backend is running");
    }

    public record HelloResponse(String message) {
    }
}
