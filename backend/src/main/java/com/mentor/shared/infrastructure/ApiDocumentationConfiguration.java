package com.mentor.shared.infrastructure;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApiDocumentationConfiguration {

    @Bean
    public OpenAPI mentorOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Mentor API")
                .version("v1")
                .description("Peer tutoring API for Universidad Central del Ecuador."));
    }
}
