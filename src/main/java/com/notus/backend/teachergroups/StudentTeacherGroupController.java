package com.notus.backend.teachergroups;

import com.notus.backend.teachergroups.dto.StudentGroupResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/student/groups")
public class StudentTeacherGroupController {

    private final GroupMembershipService membershipService;

    public StudentTeacherGroupController(GroupMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping
    public List<StudentGroupResponse> list(Principal principal) {
        return membershipService.listStudentGroups(principal.getName());
    }
}
