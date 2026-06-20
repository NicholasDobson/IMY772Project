package za.co.tuks.amrdashboard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.tuks.amrdashboard.backend.model.ImportBatch;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatch, UUID> {
    // Most recent imports first, for the "Imports" tab.
    List<ImportBatch> findAllByOrderByImportedAtDesc();
}
