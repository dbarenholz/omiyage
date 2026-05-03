package online.dbarenholz.omiyage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OmiyageApplication {
    public static void main(String[] args) {
        SpringApplication.run(OmiyageApplication.class, args);
    }
}
