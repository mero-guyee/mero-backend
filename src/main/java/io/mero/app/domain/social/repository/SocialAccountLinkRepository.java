package io.mero.app.domain.social.repository;

import io.mero.app.domain.social.entity.SocialAccountLink;
import io.mero.app.global.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountLinkRepository extends JpaRepository<SocialAccountLink, Long> {

    Optional<SocialAccountLink> findByUserIdAndProvider(Long userId, SocialProvider provider);

    boolean existsByUserIdAndProvider(Long userId, SocialProvider provider);

    void deleteByUserIdAndProvider(Long userId, SocialProvider provider);
}
