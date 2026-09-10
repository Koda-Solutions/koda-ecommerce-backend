package com.koda.ecommerce.user.components.auth.service;

import com.koda.ecommerce.user.components.keys.service.KeyRotationService;
import com.koda.ecommerce.user.components.tokens.RefreshRotation;
import com.koda.ecommerce.user.components.tokens.TokenPair;
import com.koda.ecommerce.user.components.tokens.service.TokenService;
import com.koda.ecommerce.user.model.Customer;
import com.koda.ecommerce.user.model.CustomerStatus;
import com.koda.ecommerce.user.model.SubjectType;
import com.koda.ecommerce.user.repository.CustomerRepository;
import com.koda.ecommerce.user.utils.ApiException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.koda.ecommerce.user.utils.ErrorCodes.ACCOUNT_BLOCKED;
import static com.koda.ecommerce.user.utils.ErrorCodes.BAD_CREDENTIALS;
import static com.koda.ecommerce.user.utils.ErrorCodes.EMAIL_ALREADY_EXISTS;
import static com.koda.ecommerce.user.utils.ErrorCodes.MOBILE_ALREADY_EXISTS;
import static com.koda.ecommerce.user.utils.ErrorCodes.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CustomerRepository customerRepository;
    private final TokenService tokenService;
    private final LoginGuardService loginGuardService;
    private final KeyRotationService keyRotationService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResult register(com.koda.ecommerce.user.components.auth.dto.register.RegisterRequestDTO dto) {
        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        String mobile = normalizeMobile(dto.getMobile());

        if (customerRepository.findByEmail(email).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, EMAIL_ALREADY_EXISTS,
                    "An account with this email already exists");
        }
        if (mobile != null && customerRepository.findByMobile(mobile).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, MOBILE_ALREADY_EXISTS,
                    "An account with this mobile already exists");
        }

        Customer customer = new Customer();
        customer.setFullName(dto.getFullName().trim());
        customer.setEmail(email);
        customer.setMobile(mobile);
        customer.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        customer.setPasswordUpdatedAt(LocalDateTime.now());
        customer.setKeyVersion(1);
        customer.setLanguage(dto.getLanguage());
        customer.setStatus(CustomerStatus.ACTIVE);
        customerRepository.save(customer);

        loginGuardService.record(email, null, true, null);
        TokenPair tokens = tokenService.issueForCustomer(customer);
        return new RegisterResult(tokens, customer);
    }

    @Transactional
    public TokenPair login(com.koda.ecommerce.user.components.auth.dto.login.LoginRequestDTO dto,
                           String ipAddress) {
        String identifier = normalizeIdentifier(dto.getIdentifier());
        loginGuardService.assertLoginAllowed(identifier);

        Optional<Customer> candidate = findByIdentifier(identifier);
        if (candidate.isEmpty() || !passwordEncoder.matches(dto.getPassword(),
                candidate.get().getPasswordHash())) {
            loginGuardService.record(identifier, ipAddress, false, "BAD_CREDENTIALS");
            throw new ApiException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS,
                    "Wrong email, mobile or password");
        }

        Customer customer = candidate.get();
        if (CustomerStatus.BLOCKED.equals(customer.getStatus())) {
            loginGuardService.record(identifier, ipAddress, false, "ACCOUNT_BLOCKED");
            throw new ApiException(HttpStatus.UNAUTHORIZED, ACCOUNT_BLOCKED,
                    "This account is blocked");
        }

        loginGuardService.record(identifier, ipAddress, true, null);
        return tokenService.issueForCustomer(customer);
    }

    @Transactional
    public void logout(Customer customer) {
        rotateFor(customer);
    }

    @Transactional
    public TokenPair changePassword(Customer customer,
                                    com.koda.ecommerce.user.components.auth.dto.password.PasswordRequestDTO dto) {
        if (!passwordEncoder.matches(dto.getCurrentPassword(), customer.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED,
                    "Current password is incorrect");
        }
        customer.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        customer.setPasswordUpdatedAt(LocalDateTime.now());
        rotateFor(customer);
        return tokenService.issueForCustomer(customer);
    }

    @Transactional
    public RefreshRotation refresh(String rawRefreshToken) {
        return tokenService.rotateCustomerRefresh(rawRefreshToken);
    }

    private void rotateFor(Customer customer) {
        int version = keyRotationService.rotateFor(SubjectType.CUSTOMER, customer.getId());
        customer.setKeyVersion(version);
        customerRepository.save(customer);
    }

    private Optional<Customer> findByIdentifier(String identifier) {
        Optional<Customer> byEmail = customerRepository.findByEmail(identifier);
        if (byEmail.isPresent()) {
            return byEmail;
        }
        return customerRepository.findByMobile(identifier);
    }

    private String normalizeIdentifier(String identifier) {
        String trimmed = identifier.trim();
        return trimmed.contains("@")
                ? trimmed.toLowerCase(Locale.ROOT)
                : trimmed;
    }

    private String normalizeMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return null;
        }
        return mobile.trim();
    }
}