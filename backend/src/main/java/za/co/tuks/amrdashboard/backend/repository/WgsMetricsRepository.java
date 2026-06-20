package za.co.tuks.amrdashboard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.tuks.amrdashboard.backend.model.WgsMetrics;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface WgsMetricsRepository extends JpaRepository<WgsMetrics, UUID> {
    // Find the WGS metrics for a specific isolate
    Optional<WgsMetrics> findByIsolate_IsolateId(String isolateId);

    //Fetch unique SIR Profiles for the dropdown filter
    @Query("SELECT DISTINCT w.predictedSirProfile FROM WgsMetrics w WHERE w.predictedSirProfile IS NOT NULL")
    List<String> findDistinctSirProfiles();

    long countByImportId(UUID importId);

    @Modifying
    @Query("DELETE FROM WgsMetrics w WHERE w.importId = :importId")
    void deleteByImportId(@Param("importId") UUID importId);
}