package za.co.tuks.amrdashboard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.tuks.amrdashboard.backend.model.WaterSample;
import java.util.List;

@Repository
public interface WaterSampleRepository extends JpaRepository<WaterSample, String> {
    List<WaterSample> findBySite_SiteId(String siteId);

    List<WaterSample> findBySite_SiteIdOrderByCollectionDateAsc(String siteId);

    @Query("SELECT DISTINCT ws.tripIdentifier FROM WaterSample ws WHERE ws.tripIdentifier IS NOT NULL ORDER BY ws.tripIdentifier")
    List<String> findDistinctTripIdentifiers();
}