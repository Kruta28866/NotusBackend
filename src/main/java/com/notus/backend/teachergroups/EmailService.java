package com.notus.backend.teachergroups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendGroupInvitation(String email, String groupName, String inviteLink) {
        log.info("""
                Group invitation email for {}
                Group: {}
                Invite link: {}
                Link expires in 7 days.
                """, email, groupName, inviteLink);
    }
}
