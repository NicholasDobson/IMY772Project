package za.co.tuks.amrdashboard.backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.co.tuks.amrdashboard.backend.dto.AdvisorResponse;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingService embeddingService;

    @PersistenceContext
    private EntityManager em;

    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.3;

    @SuppressWarnings("unchecked")
    public RagResult retrieve(String query) {
        if (!embeddingService.isConfigured()) {
            log.debug("[RAG] Embedding service not configured — skipping retrieval");
            return new RagResult(List.of(), "");
        }

        float[] queryEmbedding = embeddingService.embed(query);
        if (queryEmbedding == null) {
            return new RagResult(List.of(), "");
        }

        String vectorStr = formatVector(queryEmbedding);

        try {
            List<Object[]> rows = em.createNativeQuery(
                "SELECT dc.chunk_id, dc.content, dc.page_numbers, " +
                "       kd.document_id, kd.title, " +
                "       1 - (dc.embedding <=> CAST(:vec AS vector)) AS similarity " +
                "FROM document_chunks dc " +
                "JOIN knowledge_documents kd ON dc.document_id = kd.document_id " +
                "WHERE kd.status = 'READY' AND dc.embedding IS NOT NULL " +
                "ORDER BY dc.embedding <=> CAST(:vec AS vector) " +
                "LIMIT :topk"
            ).setParameter("vec", vectorStr)
             .setParameter("topk", TOP_K)
             .getResultList();

            List<RetrievedChunk> chunks = new ArrayList<>();
            for (Object[] row : rows) {
                double similarity = ((Number) row[5]).doubleValue();
                if (similarity < SIMILARITY_THRESHOLD) continue;

                chunks.add(new RetrievedChunk(
                    ((Number) row[0]).longValue(),
                    ((Number) row[3]).longValue(),
                    (String) row[4],
                    (String) row[1],
                    (String) row[2],
                    similarity
                ));
            }

            String context = buildRagContext(chunks);
            log.info("[RAG] Retrieved {} relevant chunks for query", chunks.size());
            return new RagResult(chunks, context);

        } catch (Exception e) {
            log.error("[RAG] Vector search failed: {}", e.getMessage());
            return new RagResult(List.of(), "");
        }
    }

    private String buildRagContext(List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (RetrievedChunk chunk : chunks) {
            sb.append("From \"").append(chunk.documentTitle()).append("\"");
            if (chunk.pageNumbers() != null && !chunk.pageNumbers().isBlank()) {
                sb.append(" (pages ").append(chunk.pageNumbers()).append(")");
            }
            sb.append(":\n").append(chunk.content()).append("\n\n");
        }
        return sb.toString().strip();
    }

    public List<AdvisorResponse.SourceReference> toSourceReferences(List<RetrievedChunk> chunks) {
        return chunks.stream().map(c -> new AdvisorResponse.SourceReference(
            c.documentId(),
            c.documentTitle(),
            c.content().length() > 150 ? c.content().substring(0, 150) + "..." : c.content(),
            c.pageNumbers(),
            Math.round(c.similarity() * 1000.0) / 1000.0
        )).toList();
    }

    private String formatVector(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public record RetrievedChunk(Long chunkId, Long documentId, String documentTitle,
                                  String content, String pageNumbers, double similarity) {}

    public record RagResult(List<RetrievedChunk> chunks, String context) {}
}
