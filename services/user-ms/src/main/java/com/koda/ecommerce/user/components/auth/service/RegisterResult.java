package com.koda.ecommerce.user.components.auth.service;

import com.koda.ecommerce.user.components.tokens.TokenPair;
import com.koda.ecommerce.user.model.Customer;

public record RegisterResult(TokenPair tokens, Customer customer) {
}