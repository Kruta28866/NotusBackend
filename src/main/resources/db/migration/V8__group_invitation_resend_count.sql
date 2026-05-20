ALTER TABLE group_invitations
    ADD COLUMN IF NOT EXISTS resend_count INTEGER;

UPDATE group_invitations
SET resend_count = 0
WHERE resend_count IS NULL;

UPDATE group_invitations
SET resend_count = 1
WHERE resend_count = 0
  AND last_sent_at IS NOT NULL
  AND created_at IS NOT NULL
  AND last_sent_at > created_at + INTERVAL '1 minute';

ALTER TABLE group_invitations
    ALTER COLUMN resend_count SET DEFAULT 0;
