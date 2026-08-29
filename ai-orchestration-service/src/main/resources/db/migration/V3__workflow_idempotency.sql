CREATE UNIQUE INDEX IF NOT EXISTS uq_workflow_instances_ticket_type
    ON orchestration_schema.workflow_instances(ticket_id, workflow_type);

CREATE UNIQUE INDEX IF NOT EXISTS uq_processed_event_consumer
    ON orchestration_schema.processed_events(event_id, consumer_group);
