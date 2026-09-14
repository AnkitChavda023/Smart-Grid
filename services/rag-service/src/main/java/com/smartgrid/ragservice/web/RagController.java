package com.smartgrid.ragservice.web;

import com.smartgrid.ragservice.domain.SourceType;
import com.smartgrid.ragservice.ingest.IngestionService;
import com.smartgrid.ragservice.repository.DocumentChunkRepository;
import com.smartgrid.ragservice.retrieval.HybridRetrievalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RagController {

    private final IngestionService ingestionService;
    private final HybridRetrievalService retrievalService;
    private final DocumentChunkRepository chunkRepository;

    public RagController(IngestionService ingestionService, HybridRetrievalService retrievalService, DocumentChunkRepository chunkRepository) {
        this.ingestionService = ingestionService;
        this.retrievalService = retrievalService;
        this.chunkRepository = chunkRepository;
    }

    @PostMapping("/rag/ingest")
    public IngestionService.IngestionStats ingest() {
        return ingestionService.ingestAll();
    }

    @PostMapping("/rag/search")
    public List<HybridRetrievalService.RetrievedChunk> search(@Valid @RequestBody SearchRequest request) {
        return retrievalService.search(request.query(), request.k() == null ? 5 : request.k());
    }

    @GetMapping("/rag/status")
    public RagStatusResponse status() {
        return new RagStatusResponse(
                chunkRepository.countBySourceType(SourceType.VENDOR_CAPABILITY),
                chunkRepository.countBySourceType(SourceType.CONTRACT_TERMS),
                chunkRepository.countBySourceType(SourceType.SLA_BREACH),
                chunkRepository.count());
    }

    public record SearchRequest(@NotBlank String query, @Positive Integer k) {
    }

    public record RagStatusResponse(long vendorCapabilityChunks, long contractTermsChunks, long slaBreachChunks, long totalChunks) {
    }
}
