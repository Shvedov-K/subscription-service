package com.xtended.subscriptionservice.subscription.service;

import com.xtended.subscriptionservice.subscription.model.User;
import com.xtended.subscriptionservice.subscription.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UUID testUserId;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = new User(testUserId);
    }

    @Test
    @DisplayName("Создание пользователя с валидным ID")
    void createAndReturnUserById_ValidUserId_ShouldReturnSavedUser() {
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.createAndReturnUserById(testUserId);

        assertNotNull(result);
        assertEquals(testUserId, result.getId());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Создание пользователя с null ID")
    void createAndReturnUserById_NullUserId_ShouldHandleGracefully() {
        User userWithNullId = new User(null);
        when(userRepository.save(any(User.class))).thenReturn(userWithNullId);

        User result = userService.createAndReturnUserById(null);

        assertNotNull(result);
        assertNull(result.getId());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Проверка вызова репозитория с правильными параметрами")
    void createAndReturnUserById_ValidUserId_ShouldCallRepositoryWithCorrectUser() {
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.createAndReturnUserById(testUserId);

        verify(userRepository, times(1)).save(argThat(user -> 
            user.getId().equals(testUserId)
        ));
    }
}
