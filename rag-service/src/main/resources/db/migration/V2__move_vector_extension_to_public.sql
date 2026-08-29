-- Keep the pgvector type on PostgreSQL's standard search path. Flyway runs
-- with rag_schema as its default schema, which otherwise installs the
-- extension (and its vector type) inside that application schema.
ALTER EXTENSION vector SET SCHEMA public;
