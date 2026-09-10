package com.koda.ecommerce.user.repository;

import com.koda.ecommerce.user.model.RefreshToken;
import com.koda.ecommerce.user.model.SubjectType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllBySubjectTypeAndSubjectIdAndRevokedAtIsNull(
            SubjectType subjectType, Long subjectId);
}