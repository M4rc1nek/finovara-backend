package com.finovara.securitymonitoring.login.model;

import com.finovara.securitymonitoring.clientdata.model.ClientData;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "login_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String lastLoginIp;
    private String lastLoginLocation;
    private String lastLoginBrowser;
    private LocalDateTime lastLoginAt;

    @Column(nullable = false)
    private Long loginCount = 0L;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false, unique = true)
    private Long userId;

    @OneToMany(mappedBy = "loginProfile", cascade = CascadeType.ALL)
    private List<ClientData> clientData = new ArrayList<>();

}