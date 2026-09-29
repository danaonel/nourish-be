package app.nourish.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata for the auto-generated Swagger UI. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nourishOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Nourish API")
                .version("1.0.0")
                .description("AI Personal Nutritionist backend. Send the current user via the "
                        + "X-User-Id header (defaults to demo-user).")
                .license(new License().name("Proprietary")));
    }
}
