package btvn.medicore.repository;

import btvn.medicore.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    // Tìm các token còn hiệu lực của một user
    @Query("SELECT t FROM Token t INNER JOIN t.user u WHERE u.id = :userId AND (t.expired = false OR t.revoked = false)")
    List<Token> findAllValidTokenByUser(Long userId);

    Optional<Token> findByTokenValue(String tokenValue);
}