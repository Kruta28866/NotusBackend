package com.notus.backend.users.teachercode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "teacher_codes")
@Getter
@Setter
@NoArgsConstructor
public class TeacherCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "code_hash", unique = true)
    private String codeHash;

    private String email;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(columnDefinition = "boolean default false")
    private boolean used = false;

    private Instant usedAt;

    private String usedByUserId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private String createdByAdminId;

    private Instant expiresAt;

    private Integer usageLimit;

    @Column(nullable = false)
    private int timesUsed = 0;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
