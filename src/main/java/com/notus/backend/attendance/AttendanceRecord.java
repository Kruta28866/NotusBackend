package com.notus.backend.attendance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "attendance_record",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_att_record_session_student", columnNames = {"sessionId", "studentUid"})
        },
        indexes = {
                @Index(name = "idx_att_record_session", columnList = "sessionId"),
                @Index(name = "idx_att_record_student", columnList = "studentUid")
        })
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false)
    private Long sessionId;

    @Setter
    @Column(nullable = false)
    private String studentUid;

    @Setter
    @Column(nullable = false)
    private Instant checkedInAt;

    @PrePersist
    void prePersist() {
        if (checkedInAt == null) checkedInAt = Instant.now();
    }

}
