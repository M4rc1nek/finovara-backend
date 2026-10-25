package com.finovara.authservice.user.repository;

import com.finovara.authservice.user.model.OAuthProvider;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.user.UserDataProjection;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findUserByEmail(String email);

    Optional<User> findUserByOauthProviderAndProviderUserId(OAuthProvider oauthProvider, String providerUserId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);


    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u.username) = LOWER(:username)")
    boolean existsByUsernameIgnoreCase(String username);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT new com.finovara.contracts.user.authorization.dto.UserDataDto(
                u.id,
                u.username,
                u.email,
                u.profileImagePath
            )
            FROM User u
            WHERE (LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
            AND u.hasSharedAccount = false
            AND u.id <> :userId
            ORDER BY u.username ASC
            """)
    List<UserDataDto> searchByUsernameOrEmail(String query, Long userId, Pageable pageable);

    @Query("""
                SELECT new com.finovara.contracts.user.authorization.dto.UserDataDto(
                u.id, u.username, u.email, u.profileImagePath
                )
                FROM User u
                WHERE u.id IN :userIds
            """)
    List<UserDataDto> findBasicInfoByIds(List<Long> userIds);

    @Query("SELECT u.username FROM User u WHERE u.id = :userId")
    Optional<String> findUsernameById(Long userId);

    @Query("SELECT u.email FROM User u WHERE u.id = :userId")
    Optional<String> findEmailById(Long userId);

    @Query("SELECT u.id FROM User u")
    List<Long> findAllUserIds();

    @Query("""
            SELECT u.username AS username, u.email AS email
            FROM User u
            WHERE u.id = :userId
            """)
    Optional<UserDataProjection> findUserDataById(Long userId);
}