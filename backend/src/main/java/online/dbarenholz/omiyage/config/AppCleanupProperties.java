package online.dbarenholz.omiyage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cleanup")
public record AppCleanupProperties(
        int inactiveAccountMinutes
) {
}
