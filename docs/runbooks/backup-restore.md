# Runbook: Database Backup, Point-in-Time Recovery & Restore Drill

## Objective
Provide documented and testable procedures for full and incremental database backup and disaster recovery restore.

---

## 1. Backup Procedure

### Full Logical Schema Dump
```bash
# Dump all schemas into timestamped archive
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
docker compose exec postgres pg_dump -U resolveiq -d resolveiq_db -F c -b -v -f "/var/lib/postgresql/backups/resolveiq_${TIMESTAMP}.dump"
```

---

## 2. Restore Procedure

### Restore into Staging / Drill Instance
```bash
# Terminate existing connections
docker compose exec postgres psql -U resolveiq -d resolveiq_db -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'resolveiq_db' AND pid <> pg_backend_pid();"

# Restore from dump file
docker compose exec postgres pg_restore -U resolveiq -d resolveiq_db -v -c "/var/lib/postgresql/backups/resolveiq_${TIMESTAMP}.dump"
```

---

## 3. Post-Restore Validation Checks
1. Check Flyway schema history across all 6 service schemas:
   ```sql
   SELECT schema_name, count(*) FROM information_schema.tables WHERE table_schema LIKE '%_schema' GROUP BY schema_name;
   ```
2. Verify vector index integrity:
   ```sql
   SELECT count(*) FROM rag_schema.knowledge_chunks WHERE embedding IS NOT NULL;
   ```
