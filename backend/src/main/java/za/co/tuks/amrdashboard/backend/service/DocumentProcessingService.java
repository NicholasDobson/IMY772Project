package za.co.tuks.amrdashboard.backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.tuks.amrdashboard.backend.model.DocumentChunk;
import za.co.tuks.amrdashboard.backend.model.DocumentStatus;
import za.co.tuks.amrdashboard.backend.model.KnowledgeDocument;
import za.co.tuks.amrdashboard.backend.repository.DocumentChunkRepository;
import za.co.tuks.amrdashboard.backend.repository.KnowledgeDocumentRepository;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final MinioService minioService;
    private final EmbeddingService embeddingService;
    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;

    @PersistenceContext
    private EntityManager em;

    private final Tika tika = new Tika();

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 200;

    @Async
    @Transactional
    public void processDocument(Long documentId) {
        KnowledgeDocument doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) return;

        doc.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(doc);
        log.info("[RAG] Processing document {} ({})", documentId, doc.getOriginalFilename());

        try {
            String fullText = extractText(doc.getMinioObjectKey());
            if (fullText == null || fullText.isBlank()) {
                fail(doc, "No text could be extracted from PDF");
                return;
            }

            List<ChunkData> chunks = chunkText(fullText);
            if (chunks.isEmpty()) {
                fail(doc, "No chunks generated from extracted text");
                return;
            }

            log.info("[RAG] Generated {} chunks from document {}", chunks.size(), documentId);

            List<String> chunkTexts = chunks.stream().map(ChunkData::content).collect(Collectors.toList());
            List<float[]> embeddings = embeddingService.embedBatch(chunkTexts);

            saveChunks(doc, chunks, embeddings);

            doc.setStatus(DocumentStatus.READY);
            doc.setChunkCount(chunks.size());
            doc.setProcessedAt(LocalDateTime.now());
            documentRepository.save(doc);
            log.info("[RAG] Document {} ready ({} chunks)", documentId, chunks.size());

        } catch (Exception e) {
            log.error("[RAG] Processing failed for document {}", documentId, e);
            fail(doc, e.getMessage());
        }
    }

    private String extractText(String objectKey) {
        try (InputStream is = minioService.download(objectKey)) {
            return tika.parseToString(is);
        } catch (Exception e) {
            log.error("[RAG] Text extraction failed: {}", e.getMessage());
            return null;
        }
    }

    List<ChunkData> chunkText(String text) {
        List<ChunkData> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n");

        StringBuilder current = new StringBuilder();
        int chunkIndex = 0;

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.strip();
            if (trimmed.isEmpty()) continue;

            if (current.length() + trimmed.length() + 1 > CHUNK_SIZE && current.length() > 0) {
                chunks.add(new ChunkData(current.toString().strip(), chunkIndex++, estimateTokens(current.toString())));

                String overlap = current.toString();
                current = new StringBuilder();
                if (overlap.length() > CHUNK_OVERLAP) {
                    current.append(overlap.substring(overlap.length() - CHUNK_OVERLAP));
                }
            }
            if (current.length() > 0) current.append("\n\n");
            current.append(trimmed);
        }

        if (current.length() > 0) {
            chunks.add(new ChunkData(current.toString().strip(), chunkIndex, estimateTokens(current.toString())));
        }

        return chunks;
    }

    void saveChunks(KnowledgeDocument doc, List<ChunkData> chunks, List<float[]> embeddings) {
        for (int i = 0; i < chunks.size(); i++) {
            ChunkData cd = chunks.get(i);

            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(doc);
            chunk.setChunkIndex(cd.index());
            chunk.setContent(cd.content());
            chunk.setTokenCount(cd.tokenCount());
            chunkRepository.save(chunk);
            em.flush();

            float[] embedding = (embeddings != null && i < embeddings.size()) ? embeddings.get(i) : null;
            if (embedding != null) {
                String vectorStr = formatVector(embedding);
                em.createNativeQuery(
                    "UPDATE document_chunks SET embedding = CAST(:vec AS vector) WHERE chunk_id = :id"
                ).setParameter("vec", vectorStr)
                 .setParameter("id", chunk.getChunkId())
                 .executeUpdate();
            }
        }
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

    private int estimateTokens(String text) {
        return (int) (text.length() / 4.0);
    }

    private void fail(KnowledgeDocument doc, String message) {
        doc.setStatus(DocumentStatus.FAILED);
        doc.setErrorMessage(message != null && message.length() > 2000 ? message.substring(0, 2000) : message);
        documentRepository.save(doc);
    }

    record ChunkData(String content, int index, int tokenCount) {}
}
