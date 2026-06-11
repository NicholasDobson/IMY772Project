package za.co.tuks.amrdashboard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.tuks.amrdashboard.backend.model.DocumentStatus;
import za.co.tuks.amrdashboard.backend.model.KnowledgeDocument;

import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    List<KnowledgeDocument> findAllByOrderByUploadedAtDesc();
    List<KnowledgeDocument> findByStatus(DocumentStatus status);
}
