package com.finovara.authservice.user.repository;

import com.finovara.authservice.user.dto.UserDataDto;
import com.finovara.authservice.user.model.OAuthProvider;
import com.finovara.authservice.user.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByOauthProviderAndProviderUserId(OAuthProvider oauthProvider, String providerUserId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("""
                SELECT new com.finovara.authservice.user.dto.UserDataDto(
                    u.id, 
                    u.username, 
                    u.email, 
                    u.profileImagePath
                ) 
                FROM User u 
                WHERE (u.username LIKE %:query% OR u.email LIKE %:query%) 
                  AND u.hasSharedAccount = false 
                ORDER BY u.username ASC
            """)
    List<UserDataDto> searchByUsernameOrEmail(String query, Pageable pageable);

    @Query("SELECT u.username FROM User u WHERE u.id = :userId")
    Optional<String> findUsernameById(Long userId);
}
