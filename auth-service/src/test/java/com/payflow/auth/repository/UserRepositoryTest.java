package com.payflow.auth.repository;

import com.payflow.auth.entity.Role;
import com.payflow.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .fullName("Test User")
                .role(Role.USER)
                .enabled(true)
                .build();

        savedUser = userRepository.save(user);
    }

    @Test
    @DisplayName("Find by username - should return user when username exists")
    void test_findByUsername() {
        Optional<User> found = userRepository.findByUsername("testuser");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getFullName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Find by username - should return empty when username does not exist")
    void test_findByUsername_notFound() {
        Optional<User> found = userRepository.findByUsername("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Exists by email - should return true when email exists")
    void test_existsByEmail() {
        boolean exists = userRepository.existsByEmail("test@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Exists by email - should return false when email does not exist")
    void test_existsByEmail_notFound() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Exists by username - should return true when username exists")
    void test_existsByUsername() {
        boolean exists = userRepository.existsByUsername("testuser");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Find by email - should return user when email exists")
    void test_findByEmail() {
        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }
}
