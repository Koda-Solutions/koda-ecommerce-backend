package com.koda.ecommerce.user.repository;

import com.koda.ecommerce.user.model.SigningKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SigningKeyRepository extends JpaRepository<SigningKey, Long> {

    Optional<SigningKey> findFirstByActiveTrueOrderByIdDesc();

    List<SigningKey> findAllByActiveTrue();

    Optional<SigningKey> findByKeyVersion(Integer keyVersion);
}