package com.finovara.financeservice.sharedaccount.note.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shared_notes")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SharedAccountNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String topic;
    private String description;
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long createdByUserId;
}
