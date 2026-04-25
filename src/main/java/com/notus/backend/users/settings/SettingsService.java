package com.notus.backend.users.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SettingsService {

    private final UserSettingsRepository repository;

    public SettingsService(UserSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SettingsDto getSettings(String clerkUserId) {
        UserSettings settings = repository.findByClerkUserId(clerkUserId)
                .orElseGet(() -> createDefaultSettings(clerkUserId));
        return mapToDto(settings);
    }

    @Transactional
    public SettingsDto updateSettings(String clerkUserId, SettingsDto dto) {
        UserSettings settings = repository.findByClerkUserId(clerkUserId)
                .orElseGet(() -> createDefaultSettings(clerkUserId));

        settings.setNotifyPush(dto.isNotifyPush());
        settings.setNotifyEmail(dto.isNotifyEmail());
        settings.setNotifySms(dto.isNotifySms());

        settings.setNotifyGrades(dto.isNotifyGrades());
        settings.setNotifyAnnouncements(dto.isNotifyAnnouncements());
        settings.setNotifySchedule(dto.isNotifySchedule());

        settings.setNotifyApplications(dto.isNotifyApplications());
        settings.setNotifyPayments(dto.isNotifyPayments());

        settings = repository.save(settings);
        return mapToDto(settings);
    }

    @Transactional
    public SettingsDto toggleDeactivation(String clerkUserId) {
        UserSettings settings = repository.findByClerkUserId(clerkUserId)
                .orElseGet(() -> createDefaultSettings(clerkUserId));
        settings.setDeactivated(!settings.isDeactivated());
        settings = repository.save(settings);
        return mapToDto(settings);
    }

    @Transactional
    public void markForDeletion(String clerkUserId) {
        UserSettings settings = repository.findByClerkUserId(clerkUserId)
                .orElseGet(() -> createDefaultSettings(clerkUserId));
        settings.setMarkedForDeletionAt(LocalDateTime.now().plusDays(30));
        repository.save(settings);
    }

    private UserSettings createDefaultSettings(String clerkUserId) {
        UserSettings settings = new UserSettings();
        settings.setClerkUserId(clerkUserId);
        return repository.save(settings);
    }

    private SettingsDto mapToDto(UserSettings settings) {
        return new SettingsDto(
                settings.isNotifyPush(),
                settings.isNotifyEmail(),
                settings.isNotifySms(),
                settings.isNotifyGrades(),
                settings.isNotifyAnnouncements(),
                settings.isNotifySchedule(),
                settings.isNotifyApplications(),
                settings.isNotifyPayments(),
                settings.isDeactivated()
        );
    }
}
