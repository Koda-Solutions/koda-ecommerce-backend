package com.koda.ecommerce.user.components.auth.service;

import com.koda.ecommerce.user.components.tokens.service.TokenService;
import com.koda.ecommerce.user.model.Customer;
import com.koda.ecommerce.user.model.CustomerStatus;
import com.koda.ecommerce.user.repository.CustomerRepository;
import com.koda.ecommerce.user.utils.ApiException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static com.koda.ecommerce.user.utils.ErrorCodes.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class PrincipalResolver {

    private final TokenService tokenService;
    private final CustomerRepository customerRepository;

    public Customer resolveCustomer(HttpServletRequest request) {
        String value = CookieService.read(request, CookieService.ACCESS_COOKIE);
        if (value == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED,
                    "Authentication cookie is missing");
        }
        Claims claims = tokenService.verifyAccess(value);
        long id = Long.parseLong(claims.getSubject());
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED,
                        "Customer no longer exists"));
        int tokenVersion = claims.get("ver", Integer.class);
        if (tokenVersion != customer.getKeyVersion()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED,
                    "Token was invalidated by a key rotation");
        }
        if (CustomerStatus.BLOCKED.equals(customer.getStatus())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED,
                    "Account is blocked");
        }
        return customer;
    }
}