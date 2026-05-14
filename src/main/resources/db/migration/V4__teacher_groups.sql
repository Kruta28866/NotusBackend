CREATE TABLE IF NOT EXISTS teacher_groups (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    subject VARCHAR(255),
    school_year VARCHAR(50),
    semester VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS group_memberships (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES teacher_groups(id),
    student_id BIGINT NOT NULL REFERENCES students(id),
    display_name_override VARCHAR(255),
    email_override VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    removed_at TIMESTAMP,
    CONSTRAINT uq_group_memberships_group_student UNIQUE(group_id, student_id)
);

CREATE TABLE IF NOT EXISTS group_invitations (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES teacher_groups(id),
    email VARCHAR(255),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    accepted_by_user_id BIGINT REFERENCES students(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by_teacher_id BIGINT NOT NULL REFERENCES teachers(id)
);

CREATE INDEX IF NOT EXISTS idx_teacher_groups_teacher_id ON teacher_groups(teacher_id);
CREATE INDEX IF NOT EXISTS idx_group_memberships_group_id ON group_memberships(group_id);
CREATE INDEX IF NOT EXISTS idx_group_memberships_student_id ON group_memberships(student_id);
CREATE INDEX IF NOT EXISTS idx_group_invitations_token_hash ON group_invitations(token_hash);
CREATE INDEX IF NOT EXISTS idx_group_invitations_email ON group_invitations(email);
