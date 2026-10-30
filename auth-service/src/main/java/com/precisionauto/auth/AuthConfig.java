package com.precisionauto.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    /** Demo accounts so the system is usable immediately. Disable with auth.seed-demo-users=false. */
    @Bean
    CommandLineRunner seed(UserRepository repo, PasswordEncoder enc,
                           @Value("${auth.seed-demo-users:false}") boolean seed) {
        return args -> {
            if (!seed) return;
            if (repo.findByUsername("tech1").isEmpty())
                repo.save(new UserAccount("tech1", enc.encode("Tech@12345"), Role.TECHNICIAN));
            if (repo.findByUsername("client1").isEmpty())
                repo.save(new UserAccount("client1", enc.encode("Client@12345"), Role.CLIENT));
        };
    }
}
