ALTER TABLE orchestration_schema.workflow_instances ADD COLUMN IF NOT EXISTS subject VARCHAR(500);
ALTER TABLE orchestration_schema.workflow_instances ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE orchestration_schema.workflow_instances ADD COLUMN IF NOT EXISTS priority VARCHAR(20);
ALTER TABLE orchestration_schema.workflow_instances ADD COLUMN IF NOT EXISTS channel VARCHAR(30);

UPDATE orchestration_schema.workflow_instances
SET subject = COALESCE(subject, 'Legacy workflow'),
    description = COALESCE(description, 'Input was created before replay support'),
    priority = COALESCE(priority, 'MEDIUM'),
    channel = COALESCE(channel, 'WEB');

ALTER TABLE orchestration_schema.workflow_instances ALTER COLUMN subject SET NOT NULL;
ALTER TABLE orchestration_schema.workflow_instances ALTER COLUMN description SET NOT NULL;
ALTER TABLE orchestration_schema.workflow_instances ALTER COLUMN priority SET NOT NULL;
ALTER TABLE orchestration_schema.workflow_instances ALTER COLUMN channel SET NOT NULL;
