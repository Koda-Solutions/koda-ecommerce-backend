package com.koda.ecommerce.user.components.tokens;

import com.koda.ecommerce.user.model.Customer;

public record RefreshRotation(String accessToken, String refreshToken, Customer customer) {
}