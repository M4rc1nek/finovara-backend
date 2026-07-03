package com.finovara.authservice.user.model;

import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.util.profile.ProfileImageUrlBuilder;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;

    @Column(nullable = false)
    private boolean passwordSet;
    @Column(nullable = false)
    private boolean hasSharedAccount;

    private String email;
    private LocalDateTime createdAt;
    @Column(name = "profile_image_path")
    private String profileImagePath;
    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider")
    private OAuthProvider oauthProvider;
    @Column(name = "provider_user_id")
    private String providerUserId;

    @OneToOne(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private AccountSettings accountSettings;


    public String getProfileImageUrl() {
        return ProfileImageUrlBuilder.buildProfileImageUrl(this.profileImagePath);
    }
}
