package com.smartgrid.ragservice.ingest;

import com.smartgrid.ragservice.config.RagProperties;
import com.smartgrid.ragservice.domain.DocumentChunk;
import com.smartgrid.ragservice.domain.SourceType;
import com.smartgrid.ragservice.embedding.OpenAiEmbeddingClient;
import com.smartgrid.ragservice.repository.DocumentChunkRepository;
import com.smartgrid.ragservice.search.RagChunkDocument;
import com.smartgrid.ragservice.search.RagChunkDocumentRepository;
import com.smartgrid.ragservice.vector.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class IngestionService {

    private final VendorSourceClient vendorClient;
    private final ContractSourceClient contractClient;
    private final TextChunker chunker;
    private final OpenAiEmbeddingClient embeddingClient;
    private final DocumentChunkRepository chunkRepository;
    private final VectorStore vectorStore;
    private final RagChunkDocumentRepository esRepository;
    private final RagProperties properties;

    public IngestionService(VendorSourceClient vendorClient, ContractSourceClient contractClient, TextChunker chunker,
                             OpenAiEmbeddingClient embeddingClient, DocumentChunkRepository chunkRepository,
                             VectorStore vectorStore, RagChunkDocumentRepository esRepository, RagProperties properties) {
        this.vendorClient = vendorClient;
        this.contractClient = contractClient;
        this.chunker = chunker;
        this.embeddingClient = embeddingClient;
        this.chunkRepository = chunkRepository;
        this.vectorStore = vectorStore;
        this.esRepository = esRepository;
        this.properties = properties;
    }

    public IngestionStats ingestAll() {
        int vendorChunks = 0;
        Set<String> vendorIds = new HashSet<>();
        for (VendorSourceClient.VendorCatalogEntry vendor : vendorClient.listAll()) {
            vendorIds.add(vendor.id());
            vendorChunks += ingestVendorCapability(vendor.id(), vendor.capabilities());
        }

        int contractChunks = 0;
        for (ContractSourceClient.ContractRecord contract : contractClient.listAll()) {
            vendorIds.add(contract.vendorId());
            contractChunks += ingestContractTerms(contract.id().toString(), contract.vendorId(), contract.terms());
        }

        int breachChunks = 0;
        for (String vendorId : vendorIds) {
            for (ContractSourceClient.SlaBreachRecord breach : contractClient.breachesForVendor(vendorId)) {
                breachChunks += ingestBreach(breach.id().toString(), breach.vendorId(), breach.reason());
            }
        }

        return new IngestionStats(vendorChunks, contractChunks, breachChunks);
    }

    public int ingestVendorCapability(String vendorId, String capabilities) {
        return ingestSource(SourceType.VENDOR_CAPABILITY, vendorId, vendorId, capabilities);
    }

    public int ingestContractTerms(String contractId, String vendorId, String terms) {
        return ingestSource(SourceType.CONTRACT_TERMS, contractId, vendorId, terms);
    }

    public int ingestBreach(String breachId, String vendorId, String reason) {
        return ingestSource(SourceType.SLA_BREACH, breachId, vendorId, reason);
    }

    @Transactional
    public int ingestSource(SourceType type, String sourceId, String vendorId, String text) {
        chunkRepository.deleteBySourceTypeAndSourceId(type, sourceId);
        esRepository.deleteBySourceTypeAndSourceId(type.name(), sourceId);

        if (text == null || text.isBlank()) {
            return 0;
        }
        List<String> pieces = chunker.chunk(text, properties.chunk().maxChars(), properties.chunk().overlapChars());
        if (pieces.isEmpty()) {
            return 0;
        }
        List<float[]> embeddings = embeddingClient.embed(pieces);

        for (int i = 0; i < pieces.size(); i++) {
            UUID id = UUID.randomUUID();
            chunkRepository.save(new DocumentChunk(id, type, sourceId, i, pieces.get(i), vendorId));
            vectorStore.upsertEmbedding(id, embeddings.get(i));
            esRepository.save(new RagChunkDocument(id.toString(), type.name(), sourceId, pieces.get(i), vendorId));
        }
        return pieces.size();
    }

    public record IngestionStats(int vendorChunks, int contractChunks, int breachChunks) {
        public int total() {
            return vendorChunks + contractChunks + breachChunks;
        }
    }
}
