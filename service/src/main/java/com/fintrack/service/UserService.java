package com.fintrack.service;

import com.fintrack.entity.Role;
import com.fintrack.entity.User;
import com.fintrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(String username, String password, String email) {
        if (userRepository.existsByUsername(username))
            throw new IllegalArgumentException("Username is already in use");
        if (userRepository.existsByEmail(email))
            throw new IllegalArgumentException("Email is already in use");
        User user = new User(username, passwordEncoder.encode(password), email, Role.USER);

        return userRepository.save(user);
    }
}
