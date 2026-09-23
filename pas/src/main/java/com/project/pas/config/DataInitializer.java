package com.project.pas.config;

import com.project.pas.model.Role;
import com.project.pas.model.User;
import com.project.pas.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder,
            @org.springframework.beans.factory.annotation.Value("${INITIAL_ADMIN_PASSWORD}") String initialPassword) {
        return args -> {
            // Create default admin if none exists
            if (userRepository.findByEmail("admin@pas.com").isEmpty()) {
                User admin = new User();
                admin.setFullName("System Administrator");
                admin.setEmail("admin@pas.com");
                admin.setPassword(passwordEncoder.encode(initialPassword));
                admin.setRole(Role.ADMIN);
                admin.setEnabled(true);
                // Admin has no branch
                userRepository.save(admin);
                // System.out.println("=== Default admin created: admin@pas.com ===");
            }
        };
    }
}
