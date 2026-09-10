package com.koda.ecommerce.user.repository;

import com.koda.ecommerce.user.model.LoginAttempt;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    long countByIdentifierAndCreatedAtAfter(String identifier, LocalDateTime after);
}