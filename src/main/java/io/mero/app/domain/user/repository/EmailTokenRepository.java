package io.mero.app.domain.user.repository;

import io.mero.app.domain.user.entity.EmailToken;
import io.mero.app.domain.user.entity.EmailTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    Optional<EmailToken> findByTokenAndType(String token, EmailTokenType type);

    @Modifying
    @Query("DELETE FROM EmailToken t WHERE t.email = :email AND t.type = :type")
    void deleteByEmailAndType(String email, EmailTokenType type);

    @Modifying
    @Query("DELETE FROM EmailToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);
}
