package za.co.tuks.amrdashboard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.tuks.amrdashboard.backend.model.WaterSample;
import java.util.List;
import java.util.UUID;

@Repository
public interface WaterSampleRepository extends JpaRepository<WaterSample, String> {
    List<WaterSample> findBySite_SiteId(String siteId);

    List<WaterSample> findBySite_SiteIdOrderByCollectionDateAsc(String siteId);

    @Query("SELECT DISTINCT ws.tripIdentifier FROM WaterSample ws WHERE ws.tripIdentifier IS NOT NULL ORDER BY ws.tripIdentifier")
    List<String> findDistinctTripIdentifiers();

    long countByImportId(UUID importId);

    @Modifying
    @Query("DELETE FROM WaterSample ws WHERE ws.importId = :importId")
    void deleteByImportId(@Param("importId") UUID importId);
}