package com.notus.backend.attendance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "attendance_session",
        indexes = {
                @Index(name = "idx_att_sess_teacher_uid", columnList = "teacherUid"),
                @Index(name = "idx_att_sess_active", columnList = "active")
        })
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false)
    private String teacherUid;

    @Setter
    @Column(nullable = false)
    private String title; // np. "Algorytmy - wykład 1"

    @Setter
    @Column(nullable = false)
    private Instant createdAt;

    @Setter
    @Column(nullable = false)
    private boolean active;

    // opcjonalnie: kiedy kończy się zajęcie
    private Instant endsAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

}
