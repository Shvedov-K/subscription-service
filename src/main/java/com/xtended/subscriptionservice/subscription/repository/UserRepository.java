package com.xtended.subscriptionservice.subscription.repository;

import com.xtended.subscriptionservice.subscription.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Репозитарий для работы с user
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

}
