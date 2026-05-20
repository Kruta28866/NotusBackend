ALTER TABLE students
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(32);

ALTER TABLE group_invitations
    ADD COLUMN IF NOT EXISTS last_sent_at TIMESTAMP;

UPDATE group_invitations
SET email = lower(trim(email))
WHERE email IS NOT NULL;

UPDATE group_invitations
SET last_sent_at = created_at
WHERE last_sent_at IS NULL
  AND status IN ('PENDING', 'ACCEPTED', 'CANCELLED');

WITH ranked_groups AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY teacher_id,
                            lower(name),
                            coalesce(lower(subject), ''),
                            coalesce(school_year, ''),
                            coalesce(semester, '')
               ORDER BY created_at ASC, id ASC
           ) AS rn
    FROM teacher_groups
    WHERE active = TRUE
)
UPDATE teacher_groups tg
SET active = FALSE,
    updated_at = now()
FROM ranked_groups rg
WHERE tg.id = rg.id
  AND rg.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_teacher_groups_active_identity
    ON teacher_groups (
        teacher_id,
        lower(name),
        coalesce(lower(subject), ''),
        coalesce(school_year, ''),
        coalesce(semester, '')
    )
    WHERE active = TRUE;

CREATE INDEX IF NOT EXISTS idx_group_invitations_group_email_status
    ON group_invitations (group_id, lower(email), status);
