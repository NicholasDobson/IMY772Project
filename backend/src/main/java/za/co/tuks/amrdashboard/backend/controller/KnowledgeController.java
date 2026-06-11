package za.co.tuks.amrdashboard.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.co.tuks.amrdashboard.backend.dto.KnowledgeDocumentDTO;
import za.co.tuks.amrdashboard.backend.model.DocumentStatus;
import za.co.tuks.amrdashboard.backend.model.KnowledgeDocument;
import za.co.tuks.amrdashboard.backend.repository.DocumentChunkRepository;
import za.co.tuks.amrdashboard.backend.repository.KnowledgeDocumentRepository;
import za.co.tuks.amrdashboard.backend.service.DocumentProcessingService;
import za.co.tuks.amrdashboard.backend.service.MinioService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/knowledge/documents")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final MinioService minioService;
    private final DocumentProcessingService processingService;

    @GetMapping
    public ResponseEntity<List<KnowledgeDocumentDTO>> listDocuments() {
        List<KnowledgeDocumentDTO> docs = documentRepository.findAllByOrderByUploadedAtDesc()
                .stream().map(this::toDTO).toList();
        return ResponseEntity.ok(docs);
    }

    @PostMapping
    public ResponseEntity<Object> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }
        if (!isPdf(file)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only PDF files are accepted"));
        }

        try {
            return ResponseEntity.accepted().body(toDTO(storeAndProcess(file, title)));
        } catch (Exception e) {
            log.error("Document upload failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<Object> uploadBatch(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "No files provided"));
        }

        List<KnowledgeDocumentDTO> accepted = new ArrayList<>();
        List<Map<String, String>> rejected = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty() || !isPdf(file)) {
                rejected.add(Map.of(
                    "filename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                    "error", file.isEmpty() ? "Empty file" : "Not a PDF"));
                continue;
            }
            try {
                accepted.add(toDTO(storeAndProcess(file, null)));
            } catch (Exception e) {
                log.error("Batch upload failed for {}", file.getOriginalFilename(), e);
                rejected.add(Map.of(
                    "filename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                    "error", e.getMessage()));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("accepted", accepted);
        result.put("rejected", rejected);
        return ResponseEntity.accepted().body(result);
    }

    private boolean isPdf(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.equals("application/pdf");
    }

    private KnowledgeDocument storeAndProcess(MultipartFile file, String title) throws Exception {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
        if (title == null || title.isBlank()) {
            title = originalFilename.replaceAll("\\.pdf$", "");
        }

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(title);
        doc.setOriginalFilename(originalFilename);
        doc.setFileSizeBytes(file.getSize());
        doc.setContentType(file.getContentType());
        doc.setStatus(DocumentStatus.UPLOADING);
        doc.setUploadedAt(LocalDateTime.now());

        String objectKey = "documents/" + UUID.randomUUID() + "/" + originalFilename;
        doc.setMinioObjectKey(objectKey);

        minioService.upload(objectKey, file.getInputStream(), file.getSize(), file.getContentType());
        doc.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(doc);

        processingService.processDocument(doc.getDocumentId());
        return doc;
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Object> deleteDocument(@PathVariable Long documentId) {
        KnowledgeDocument doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            minioService.delete(doc.getMinioObjectKey());
        } catch (Exception e) {
            log.warn("MinIO delete failed for {}: {}", doc.getMinioObjectKey(), e.getMessage());
        }

        documentRepository.delete(doc);
        return ResponseEntity.ok(Map.of("deleted", documentId));
    }

    @GetMapping("/{documentId}/status")
    public ResponseEntity<Object> getStatus(@PathVariable Long documentId) {
        KnowledgeDocument doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "status", doc.getStatus().name(),
            "chunkCount", doc.getChunkCount() != null ? doc.getChunkCount() : 0,
            "errorMessage", doc.getErrorMessage() != null ? doc.getErrorMessage() : ""
        ));
    }

    @PostMapping("/{documentId}/reprocess")
    public ResponseEntity<Object> reprocess(@PathVariable Long documentId) {
        KnowledgeDocument doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        chunkRepository.deleteByDocumentDocumentId(documentId);
        doc.setStatus(DocumentStatus.PROCESSING);
        doc.setErrorMessage(null);
        doc.setChunkCount(null);
        doc.setProcessedAt(null);
        documentRepository.save(doc);

        processingService.processDocument(documentId);
        return ResponseEntity.accepted().body(toDTO(doc));
    }

    private KnowledgeDocumentDTO toDTO(KnowledgeDocument doc) {
        return new KnowledgeDocumentDTO(
            doc.getDocumentId(),
            doc.getTitle(),
            doc.getOriginalFilename(),
            doc.getStatus().name(),
            doc.getErrorMessage(),
            doc.getChunkCount(),
            doc.getFileSizeBytes(),
            doc.getUploadedBy(),
            doc.getUploadedAt(),
            doc.getProcessedAt()
        );
    }
}
