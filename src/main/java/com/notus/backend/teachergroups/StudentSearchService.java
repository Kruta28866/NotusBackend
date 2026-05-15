package com.notus.backend.teachergroups;

import com.notus.backend.teachergroups.dto.StudentSearchResponse;
import com.notus.backend.users.Role;
import com.notus.backend.users.Student;
import com.notus.backend.users.StudentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentSearchService {

    private static final int MIN_QUERY_LENGTH = 3;
    private static final int MAX_RESULTS = 10;

    private final TeacherGroupService groupService;
    private final StudentRepository studentRepository;
    private final GroupMembershipRepository membershipRepository;

    public StudentSearchService(TeacherGroupService groupService,
                                StudentRepository studentRepository,
                                GroupMembershipRepository membershipRepository) {
        this.groupService = groupService;
        this.studentRepository = studentRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentSearchResponse> search(String teacherUid, Long groupId, String email) {
        TeacherGroup group = groupService.requireOwnedGroup(teacherUid, groupId);
        String query = normalize(email);
        if (query.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }

        return studentRepository.findByRoleAndEmailContainingIgnoreCaseOrderByEmailAsc(
                        Role.STUDENT,
                        query,
                        PageRequest.of(0, MAX_RESULTS)
                )
                .stream()
                .map(student -> toResponse(group, student))
                .toList();
    }

    private StudentSearchResponse toResponse(TeacherGroup group, Student student) {
        boolean alreadyInGroup = membershipRepository.existsByGroupAndStudentAndStatus(
                group,
                student,
                GroupMembershipStatus.ACTIVE
        );
        return new StudentSearchResponse(
                student.getId(),
                student.getName(),
                student.getEmail(),
                alreadyInGroup
        );
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
