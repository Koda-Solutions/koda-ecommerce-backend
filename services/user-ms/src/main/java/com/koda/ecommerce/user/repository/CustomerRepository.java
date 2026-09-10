package com.koda.ecommerce.user.repository;

import com.koda.ecommerce.user.model.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByMobile(String mobile);

    Optional<Customer> findByEmailOrMobile(String email, String mobile);
}