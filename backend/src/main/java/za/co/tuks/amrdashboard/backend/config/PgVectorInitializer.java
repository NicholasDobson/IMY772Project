package za.co.tuks.amrdashboard.backend.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.tuks.amrdashboard.backend.model.DocumentStatus;
import za.co.tuks.amrdashboard.backend.model.KnowledgeDocument;
import za.co.tuks.amrdashboard.backend.repository.KnowledgeDocumentRepository;
import za.co.tuks.amrdashboard.backend.service.DocumentProcessingService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PgVectorInitializer {

    @PersistenceContext
    private EntityManager em;

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentProcessingService processingService;

    @Value("${embedding.dimensions:1024}")
    private int dimensions;

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            em.createNativeQuery("CREATE EXTENSION IF NOT EXISTS vector").executeUpdate();
            log.info("[pgvector] Extension enabled");
        } catch (Exception e) {
            log.warn("[pgvector] Could not create extension (may need superuser): {}", e.getMessage());
        }

        try {
            em.createNativeQuery(
                "DO $$ BEGIN " +
                "  IF NOT EXISTS (SELECT 1 FROM information_schema.columns " +
                "    WHERE table_name = 'document_chunks' AND column_name = 'embedding') THEN " +
                "    ALTER TABLE document_chunks ADD COLUMN embedding vector(" + dimensions + "); " +
                "  END IF; " +
                "END $$"
            ).executeUpdate();
            log.info("[pgvector] Embedding column ensured ({}d)", dimensions);
        } catch (Exception e) {
            log.warn("[pgvector] Could not create embedding column: {}", e.getMessage());
        }

        try {
            em.createNativeQuery(
                "CREATE INDEX IF NOT EXISTS idx_chunk_embedding_hnsw " +
                "ON document_chunks USING hnsw (embedding vector_cosine_ops)"
            ).executeUpdate();
            log.info("[pgvector] HNSW index ensured");
        } catch (Exception e) {
            log.warn("[pgvector] Could not create HNSW index: {}", e.getMessage());
        }

        reprocessDocsMissingEmbeddings();
    }

    private void reprocessDocsMissingEmbeddings() {
        try {
            @SuppressWarnings("unchecked")
            List<Long> docIds = em.createNativeQuery(
                "SELECT DISTINCT kd.document_id FROM knowledge_documents kd " +
                "JOIN document_chunks dc ON dc.document_id = kd.document_id " +
                "WHERE kd.status = 'READY' AND dc.embedding IS NULL"
            ).getResultList();

            if (!docIds.isEmpty()) {
                log.info("[pgvector] Found {} documents with missing embeddings — reprocessing", docIds.size());
                for (Long docId : docIds) {
                    processingService.processDocument(docId);
                }
            }
        } catch (Exception e) {
            log.warn("[pgvector] Could not check for missing embeddings: {}", e.getMessage());
        }
    }
}
