package com.notus.backend.users.settings;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings", indexes = {
        @Index(name = "idx_user_settings_clerk_user_id", columnList = "clerk_user_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clerk_user_id", nullable = false, unique = true)
    private String clerkUserId;

    // Channels
    private boolean notifyPush = true;
    private boolean notifyEmail = true;
    private boolean notifySms = false;

    // Academic
    private boolean notifyGrades = true;
    private boolean notifyAnnouncements = true;
    private boolean notifySchedule = true;

    // Admin
    private boolean notifyApplications = true;
    private boolean notifyPayments = false;

    // Account state
    private boolean deactivated = false;
    private LocalDateTime markedForDeletionAt = null;
}
