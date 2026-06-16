package online.dbarenholz.omiyage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
@Profile("!ci")
public class TestOmiyageApplication {

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource") // spring manages this bean, we don't have to worry
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"))
                .withDatabaseName("omiyage_test")
                .withUsername("test")
                .withPassword("test");
    }

    public static void main(String[] args) {
        SpringApplication.from(OmiyageApplication::main).with(TestOmiyageApplication.class).run(args);
    }
}
