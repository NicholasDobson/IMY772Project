package za.co.tuks.amrdashboard.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "knowledge_documents")
@Data
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "minio_object_key", nullable = false, length = 1000)
    private String minioObjectKey;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DocumentStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentChunk> chunks;
}
