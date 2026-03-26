package com.notus.backend.attendance;

import com.notus.backend.users.Teacher;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "attendance_session",
        indexes = {
                @Index(name = "idx_att_sess_teacher_id", columnList = "teacher_id"),
                @Index(name = "idx_att_sess_active", columnList = "active")
        })
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(nullable = false)
    private String title;

    @Column(name = "short_code", unique = true, length = 8)
    private String shortCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean active;

    // opcjonalnie: kiedy kończy się zajęcia
    @Column(name = "ends_at")
    private Instant endsAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }




}
