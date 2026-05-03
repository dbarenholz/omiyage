package online.dbarenholz.omiyage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var mapping = registry.addMapping("/api/**")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        String[] origins = allowedOrigins.split(",");
        boolean isWildcard = origins.length == 1 && "*".equals(origins[0].trim());

        if (isWildcard) {
            // allowedOriginPatterns("*") supports credentials with any origin (reflects Origin header)
            mapping.allowedOriginPatterns("*");
        } else {
            mapping.allowedOrigins(origins);
        }
    }
}
