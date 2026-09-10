package com.koda.ecommerce.user.components.auth.dto.me;

import com.koda.ecommerce.user.model.Customer;
import lombok.Data;

@Data
public class CustomerResponseDTO {

    private Long id;
    private String fullName;
    private String email;
    private String mobile;
    private String language;
    private String status;

    public static CustomerResponseDTO from(Customer customer) {
        CustomerResponseDTO dto = new CustomerResponseDTO();
        dto.setId(customer.getId());
        dto.setFullName(customer.getFullName());
        dto.setEmail(customer.getEmail());
        dto.setMobile(customer.getMobile());
        dto.setLanguage(customer.getLanguage());
        dto.setStatus(customer.getStatus().name());
        return dto;
    }
}