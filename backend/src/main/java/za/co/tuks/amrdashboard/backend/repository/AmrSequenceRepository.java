package za.co.tuks.amrdashboard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.tuks.amrdashboard.backend.model.AmrSequence;

import java.util.List;
import java.util.UUID;

@Repository
public interface AmrSequenceRepository extends JpaRepository<AmrSequence, UUID> {
    // Easily fetch all resistance genes for a specific isolate
    List<AmrSequence> findByIsolate_IsolateId(String isolateId);

    // Find sequences by resistance class (e.g., "BETA-LACTAM")
    List<AmrSequence> findByResistanceClass(String resistanceClass);

    long countByImportId(UUID importId);

    // Bulk delete (JPQL) bypasses entity cascade, so rollback order is fully controlled.
    @Modifying
    @Query("DELETE FROM AmrSequence a WHERE a.importId = :importId")
    void deleteByImportId(@Param("importId") UUID importId);
}