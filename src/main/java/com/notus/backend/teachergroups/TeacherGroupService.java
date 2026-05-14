package com.notus.backend.teachergroups;

import com.notus.backend.teachergroups.dto.CreateTeacherGroupRequest;
import com.notus.backend.teachergroups.dto.TeacherGroupDetailsResponse;
import com.notus.backend.teachergroups.dto.TeacherGroupResponse;
import com.notus.backend.teachergroups.dto.UpdateTeacherGroupRequest;
import com.notus.backend.users.Teacher;
import com.notus.backend.users.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class TeacherGroupService {

    private final TeacherGroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final UserService userService;

    public TeacherGroupService(TeacherGroupRepository groupRepository,
                               GroupMembershipRepository membershipRepository,
                               UserService userService) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<TeacherGroupResponse> listGroups(String teacherUid) {
        Teacher teacher = currentTeacher(teacherUid);
        return groupRepository.findByTeacherAndActiveTrueOrderByCreatedAtDesc(teacher)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherGroupDetailsResponse getDetails(String teacherUid, Long groupId) {
        return toDetails(requireOwnedGroup(teacherUid, groupId));
    }

    @Transactional
    public TeacherGroupResponse create(String teacherUid, CreateTeacherGroupRequest request) {
        TeacherGroup group = new TeacherGroup();
        group.setTeacher(currentTeacher(teacherUid));
        apply(group, request.name(), request.description(), request.subject(), request.schoolYear(), request.semester());
        return toResponse(groupRepository.save(group));
    }

    @Transactional
    public TeacherGroupResponse update(String teacherUid, Long groupId, UpdateTeacherGroupRequest request) {
        TeacherGroup group = requireOwnedGroup(teacherUid, groupId);
        apply(group, request.name(), request.description(), request.subject(), request.schoolYear(), request.semester());
        group.setUpdatedAt(Instant.now());
        return toResponse(groupRepository.save(group));
    }

    @Transactional
    public void delete(String teacherUid, Long groupId) {
        TeacherGroup group = requireOwnedGroup(teacherUid, groupId);
        group.setActive(false);
        group.setUpdatedAt(Instant.now());
        groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public TeacherGroup requireOwnedGroup(String teacherUid, Long groupId) {
        Teacher teacher = currentTeacher(teacherUid);
        return groupRepository.findByIdAndTeacherAndActiveTrue(groupId, teacher)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Nie masz uprawnień do tej grupy."));
    }

    public Teacher currentTeacher(String teacherUid) {
        return userService.findTeacherByUid(teacherUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Wymagane konto nauczyciela."));
    }

    private void apply(TeacherGroup group, String name, String description, String subject, String schoolYear, String semester) {
        group.setName(trimRequired(name, "Nazwa grupy jest wymagana."));
        group.setDescription(trimToNull(description));
        group.setSubject(trimToNull(subject));
        group.setSchoolYear(trimToNull(schoolYear));
        group.setSemester(trimToNull(semester));
    }

    private TeacherGroupResponse toResponse(TeacherGroup group) {
        return new TeacherGroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getSubject(),
                group.getSchoolYear(),
                group.getSemester(),
                membershipRepository.countByGroupAndStatus(group, GroupMembershipStatus.ACTIVE),
                group.getCreatedAt()
        );
    }

    private TeacherGroupDetailsResponse toDetails(TeacherGroup group) {
        return new TeacherGroupDetailsResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getSubject(),
                group.getSchoolYear(),
                group.getSemester(),
                membershipRepository.countByGroupAndStatus(group, GroupMembershipStatus.ACTIVE)
        );
    }

    private String trimRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
