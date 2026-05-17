ALTER TABLE schedule
    ADD COLUMN IF NOT EXISTS teacher_group_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_schedule_teacher_group'
    ) THEN
        ALTER TABLE schedule
            ADD CONSTRAINT fk_schedule_teacher_group
            FOREIGN KEY (teacher_group_id) REFERENCES teacher_groups(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_schedule_teacher_group_id
    ON schedule(teacher_group_id);
