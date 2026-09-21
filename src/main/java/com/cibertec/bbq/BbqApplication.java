package com.cibertec.bbq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

// Sin UserDetailsService: la autenticación es por JWT (evita el usuario/contraseña generados por Spring)
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableJpaAuditing
@EnableScheduling
public class BbqApplication {
    public static void main(String[] args) {
        SpringApplication.run(BbqApplication.class, args);
    }
}
