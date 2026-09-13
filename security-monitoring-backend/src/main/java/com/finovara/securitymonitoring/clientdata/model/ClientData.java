package com.finovara.securitymonitoring.clientdata.model;

import com.finovara.securitymonitoring.login.model.LoginProfile;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "client_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String knownIpAddress;
    private String knownLocation;
    private String knownBrowser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "login_profile_id", nullable = false)
    private LoginProfile loginProfile;

}
