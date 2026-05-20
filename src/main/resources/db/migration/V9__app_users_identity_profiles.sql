CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    auth_user_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    phone_number VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_app_users_auth_user_id
    ON app_users (auth_user_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_app_users_email
    ON app_users (email);

ALTER TABLE students
    ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE teachers
    ADD COLUMN IF NOT EXISTS user_id BIGINT;

INSERT INTO app_users (auth_user_id, email, name, role, phone_number, created_at, updated_at)
SELECT t.clerk_user_id,
       lower(trim(t.email)),
       t.name,
       'TEACHER',
       NULL,
       now(),
       now()
FROM teachers t
WHERE t.clerk_user_id IS NOT NULL
  AND t.email IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO app_users (auth_user_id, email, name, role, phone_number, created_at, updated_at)
SELECT s.clerk_user_id,
       lower(trim(s.email)),
       s.name,
       'STUDENT',
       s.phone_number,
       now(),
       now()
FROM students s
WHERE s.clerk_user_id IS NOT NULL
  AND s.email IS NOT NULL
ON CONFLICT DO NOTHING;

UPDATE teachers t
SET user_id = au.id
FROM app_users au
WHERE t.user_id IS NULL
  AND au.role = 'TEACHER'
  AND (
      au.auth_user_id = t.clerk_user_id
      OR lower(au.email) = lower(trim(t.email))
  );

UPDATE students s
SET user_id = au.id
FROM app_users au
WHERE s.user_id IS NULL
  AND au.role = 'STUDENT'
  AND (
      au.auth_user_id = s.clerk_user_id
      OR lower(au.email) = lower(trim(s.email))
  );

CREATE UNIQUE INDEX IF NOT EXISTS uq_students_user_id
    ON students (user_id)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_teachers_user_id
    ON teachers (user_id)
    WHERE user_id IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_students_app_users'
    ) THEN
        ALTER TABLE students
            ADD CONSTRAINT fk_students_app_users
            FOREIGN KEY (user_id) REFERENCES app_users(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_teachers_app_users'
    ) THEN
        ALTER TABLE teachers
            ADD CONSTRAINT fk_teachers_app_users
            FOREIGN KEY (user_id) REFERENCES app_users(id);
    END IF;
END $$;
