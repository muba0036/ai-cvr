package dk.skole.ansoegning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Startpunktet for hele appen. Tryk ▶ her i IntelliJ for at starte.
 * @SpringBootApplication får Spring til at finde og oprette alle klasser med
 * @Service, @Component, @Controller osv. i denne pakke og underpakkerne.
 */
@SpringBootApplication
public class PraktikAnsoegningApplication {
    public static void main(String[] args) {
        // Starter webserveren (Tomcat) på port 8081 og hele Spring-maskineriet
        SpringApplication.run(PraktikAnsoegningApplication.class, args);
    }
}
