package io.mero.app.domain.user.repository;

import io.mero.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByAppleId(String appleId);

    Optional<User> findByGoogleId(String googleId);

}
