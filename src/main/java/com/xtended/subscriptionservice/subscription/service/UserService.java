package com.xtended.subscriptionservice.subscription.service;

import com.xtended.subscriptionservice.subscription.model.User;
import com.xtended.subscriptionservice.subscription.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Сервис для работы с user
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public User createAndReturnUserById(UUID userId) {
        return userRepository.save(new User(userId));
    }
}
