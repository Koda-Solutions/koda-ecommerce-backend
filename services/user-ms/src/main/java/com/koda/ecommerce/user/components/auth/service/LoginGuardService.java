package com.koda.ecommerce.user.components.auth.service;

import com.koda.ecommerce.user.model.LoginAttempt;
import com.koda.ecommerce.user.repository.LoginAttemptRepository;
import com.koda.ecommerce.user.utils.ApiException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.koda.ecommerce.user.utils.ErrorCodes.RATE_LIMITED;

@Service
@RequiredArgsConstructor
public class LoginGuardService {

    private final LoginAttemptRepository loginAttemptRepository;

    @Value("${auth.rate-limit.max-failures}")
    private long maxFailures;

    @Value("${auth.rate-limit.window-minutes}")
    private long windowMinutes;

    public void assertLoginAllowed(String identifier) {
        long failed = loginAttemptRepository
                .countByIdentifierAndCreatedAtAfter(identifier,
                        LocalDateTime.now().minusMinutes(windowMinutes));
        if (failed >= maxFailures) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, RATE_LIMITED,
                    "Too many failed login attempts, try again later");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String identifier, String ipAddress, boolean success, String reason) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setIdentifier(identifier);
        attempt.setIpAddress(ipAddress);
        attempt.setSuccess(success);
        attempt.setReason(reason);
        loginAttemptRepository.save(attempt);
    }
}