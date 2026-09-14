package com.smartgrid.ragservice.vector;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class VectorStore {

    private final JdbcTemplate jdbcTemplate;

    public VectorStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertEmbedding(UUID chunkId, float[] embedding) {
        jdbcTemplate.update(
                "UPDATE document_chunks SET embedding = ?::vector WHERE id = ?",
                toVectorLiteral(embedding), chunkId);
    }

    public List<VectorHit> similaritySearch(float[] queryEmbedding, int k) {
        String literal = toVectorLiteral(queryEmbedding);
        return jdbcTemplate.query(
                """
                SELECT id, embedding <=> ?::vector AS distance
                FROM document_chunks
                WHERE embedding IS NOT NULL
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """,
                (rs, rowNum) -> new VectorHit(UUID.fromString(rs.getString("id")), rs.getDouble("distance")),
                literal, literal, k);
    }

    static String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder(embedding.length * 8);
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
