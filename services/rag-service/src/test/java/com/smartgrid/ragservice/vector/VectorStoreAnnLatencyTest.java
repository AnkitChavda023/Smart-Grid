package com.smartgrid.ragservice.vector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Populates 10,000 synthetic vectors directly via JDBC (no embedding API needed â€” index query
 * latency depends on vector cardinality, not on where the vectors came from) to verify the
 * module's "<100ms ANN query at 10K vectors" done-condition against the real IVFFlat index.
 */
@SpringBootTest
class VectorStoreAnnLatencyTest {

    private static final int VECTOR_COUNT = 10_000;
    private static final int DIMENSIONS = 1536;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VectorStore vectorStore;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM document_chunks WHERE source_id LIKE 'ann-bench-%'");
    }

    @Test
    void annQueryCompletesUnder100msFor10kVectors() {
        Random random = new Random(42);
        Instant now = Instant.now();

        jdbcTemplate.batchUpdate(
                "INSERT INTO document_chunks (id, source_type, source_id, chunk_index, content, embedding, created_at, updated_at) " +
                        "VALUES (?, 'VENDOR_CAPABILITY', ?, 0, 'synthetic benchmark chunk', ?::vector, ?, ?)",
                new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                        float[] vector = randomUnitVector(random);
                        ps.setObject(1, UUID.randomUUID());
                        ps.setString(2, "ann-bench-" + i);
                        ps.setString(3, VectorStore.toVectorLiteral(vector));
                        ps.setObject(4, java.sql.Timestamp.from(now));
                        ps.setObject(5, java.sql.Timestamp.from(now));
                    }

                    @Override
                    public int getBatchSize() {
                        return VECTOR_COUNT;
                    }
                });

        jdbcTemplate.execute("ANALYZE document_chunks");

        // Warm-up: pays JDBC connection acquisition and query-plan caching costs, so timed runs
        // reflect steady-state index performance rather than first-call overhead.
        for (int i = 0; i < 5; i++) {
            vectorStore.similaritySearch(randomUnitVector(random), 10);
        }

        // Median of several samples, not a single sample â€” a lone GC pause or OS scheduling
        // hiccup on a shared dev machine shouldn't fail a query-plan/index correctness check.
        int sampleCount = 9;
        long[] samplesMs = new long[sampleCount];
        List<VectorHit> lastHits = null;
        for (int i = 0; i < sampleCount; i++) {
            float[] queryVector = randomUnitVector(random);
            long start = System.nanoTime();
            lastHits = vectorStore.similaritySearch(queryVector, 10);
            samplesMs[i] = (System.nanoTime() - start) / 1_000_000;
        }
        java.util.Arrays.sort(samplesMs);
        long medianMs = samplesMs[sampleCount / 2];

        assertThat(lastHits).hasSize(10);
        assertThat(medianMs).isLessThan(100);
    }

    private float[] randomUnitVector(Random random) {
        float[] vector = new float[DIMENSIONS];
        double sumSquares = 0;
        for (int i = 0; i < DIMENSIONS; i++) {
            vector[i] = (float) random.nextGaussian();
            sumSquares += vector[i] * vector[i];
        }
        float norm = (float) Math.sqrt(sumSquares);
        for (int i = 0; i < DIMENSIONS; i++) {
            vector[i] /= norm;
        }
        return vector;
    }
}
