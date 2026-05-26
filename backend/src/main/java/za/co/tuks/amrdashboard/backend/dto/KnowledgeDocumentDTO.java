package za.co.tuks.amrdashboard.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeDocumentDTO {
    private Long documentId;
    private String title;
    private String originalFilename;
    private String status;
    private String errorMessage;
    private Integer chunkCount;
    private Long fileSizeBytes;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
}
