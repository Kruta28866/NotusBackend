package com.notus.backend.users.settings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingsDto {
    private boolean notifyPush;
    private boolean notifyEmail;
    private boolean notifySms;

    private boolean notifyGrades;
    private boolean notifyAnnouncements;
    private boolean notifySchedule;

    private boolean notifyApplications;
    private boolean notifyPayments;

    private boolean deactivated;
}
