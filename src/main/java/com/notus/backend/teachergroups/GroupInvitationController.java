package com.notus.backend.teachergroups;

import com.notus.backend.teachergroups.dto.AcceptGroupInvitationRequest;
import com.notus.backend.teachergroups.dto.AcceptGroupInvitationResponse;
import com.notus.backend.teachergroups.dto.GroupInvitationPreviewResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/group-invitations")
public class GroupInvitationController {

    private final GroupInvitationService invitationService;
    private final GroupMembershipService membershipService;

    public GroupInvitationController(GroupInvitationService invitationService,
                                     GroupMembershipService membershipService) {
        this.invitationService = invitationService;
        this.membershipService = membershipService;
    }

    @GetMapping("/preview")
    public GroupInvitationPreviewResponse preview(@RequestParam String token) {
        return invitationService.preview(token);
    }

    @PostMapping("/accept")
    public AcceptGroupInvitationResponse accept(Principal principal,
                                                @Valid @RequestBody AcceptGroupInvitationRequest request) {
        return membershipService.accept(principal.getName(), request);
    }
}
