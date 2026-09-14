CREATE EXTENSION IF NOT EXISTS vector;

-- Dimension must match smartgrid.rag.openai.embedding-dimensions (application.yml).
-- Default is 768 to match the local Ollama "nomic-embed-text" model. If you switch back to
-- OpenAI's text-embedding-3-small (1536-dim), this column - on an *existing* database, since
-- CREATE TABLE IF NOT EXISTS below won't alter an already-created table - needs a real
-- migration, not just an application.yml change:
--   TRUNCATE document_chunks;  -- old and new embeddings are from different models, not comparable
--   ALTER TABLE document_chunks ALTER COLUMN embedding TYPE vector(1536);
-- then re-run POST /rag/ingest to regenerate embeddings with the new model.
CREATE TABLE IF NOT EXISTS document_chunks (
    id UUID PRIMARY KEY,
    source_type VARCHAR(32) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    vendor_id VARCHAR(64),
    embedding VECTOR(768),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (source_type, source_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_document_chunks_source ON document_chunks (source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_document_chunks_vendor ON document_chunks (vendor_id);

-- IVFFlat needs at least a handful of rows to train on; harmless to (re)build on an empty/small table at this scale.
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding ON document_chunks
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 10);
