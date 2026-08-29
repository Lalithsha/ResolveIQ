package com.resolveiq.auth.application.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.Set;

@Service
public class PasswordService {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void validate(String password) {
        if (password == null || password.length() < 12 || password.length() > 128
            || !password.matches(".*[a-z].*") || !password.matches(".*[A-Z].*")
            || !password.matches(".*\\d.*") || !password.matches(".*[^A-Za-z0-9].*")) {
            throw new IllegalArgumentException("Password must be 12-128 characters and include upper, lower, number, and symbol");
        }
        String normalized = password.toLowerCase(Locale.ROOT);
        if (Set.of("password123!", "password1234!", "qwerty123456!", "admin123456!").contains(normalized)) {
            throw new IllegalArgumentException("Password is too common");
        }
    }
}
