package com.koda.ecommerce.user.components.auth.controller;

import com.koda.ecommerce.user.components.auth.dto.me.CustomerResponseDTO;
import com.koda.ecommerce.user.components.auth.service.PrincipalResolver;
import com.koda.ecommerce.user.model.Customer;
import com.koda.ecommerce.user.utils.ReturnObject;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final PrincipalResolver principalResolver;

    @GetMapping("/me")
    public ResponseEntity<ReturnObject<CustomerResponseDTO>> me(HttpServletRequest request) {
        Customer customer = principalResolver.resolveCustomer(request);
        return ResponseEntity.ok(ReturnObject.ok("Profile", CustomerResponseDTO.from(customer)));
    }
}