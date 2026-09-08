
package com.project.pas;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String hash = encoder.encode("Admin@1234");

        System.out.println("BCrypt Hash:");
        System.out.println(hash);
    }
}