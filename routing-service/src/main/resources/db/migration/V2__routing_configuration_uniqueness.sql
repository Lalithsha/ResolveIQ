-- Routing configuration is tenant-scoped. Historical development seeds used
-- random identifiers, which allowed logically identical SLA policies and rules
-- to accumulate. Collapse those duplicates before enforcing the business keys.

WITH ranked_policies AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY tenant_id, priority
               ORDER BY created_at ASC, id ASC
           ) AS duplicate_rank
    FROM routing_schema.sla_policies
)
DELETE FROM routing_schema.sla_policies policy
USING ranked_policies ranked
WHERE policy.id = ranked.id
  AND ranked.duplicate_rank > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sla_policy_tenant_priority
    ON routing_schema.sla_policies (tenant_id, priority);

-- Preserve routing decisions by moving references to the canonical rule before
-- removing duplicate name/version records.
WITH ranked_rules AS (
    SELECT id,
           FIRST_VALUE(id) OVER (
               PARTITION BY tenant_id, name, version
               ORDER BY created_at ASC, id ASC
           ) AS canonical_id,
           ROW_NUMBER() OVER (
               PARTITION BY tenant_id, name, version
               ORDER BY created_at ASC, id ASC
           ) AS duplicate_rank
    FROM routing_schema.routing_rules
), duplicate_rules AS (
    SELECT id, canonical_id
    FROM ranked_rules
    WHERE duplicate_rank > 1
)
UPDATE routing_schema.routing_decisions decision
SET matched_rule_id = duplicate.canonical_id
FROM duplicate_rules duplicate
WHERE decision.matched_rule_id = duplicate.id;

WITH ranked_rules AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY tenant_id, name, version
               ORDER BY created_at ASC, id ASC
           ) AS duplicate_rank
    FROM routing_schema.routing_rules
)
DELETE FROM routing_schema.routing_rules rule
USING ranked_rules ranked
WHERE rule.id = ranked.id
  AND ranked.duplicate_rank > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_routing_rule_tenant_name_version
    ON routing_schema.routing_rules (tenant_id, name, version);
