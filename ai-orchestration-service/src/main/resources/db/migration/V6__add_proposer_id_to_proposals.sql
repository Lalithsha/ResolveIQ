-- V6__add_proposer_id_to_proposals.sql
-- Add proposer_id to resolution_action_proposals for two-person rule enforcement

ALTER TABLE orchestration_schema.resolution_action_proposals ADD COLUMN IF NOT EXISTS proposer_id UUID;
