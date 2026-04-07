package za.co.tuks.amrdashboard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.tuks.amrdashboard.backend.model.WaterSample;
import java.util.UUID;
import java.util.List;

@Repository
public interface WaterSampleRepository extends JpaRepository<WaterSample, UUID> {
    // Custom query method generated automatically by Spring
    List<WaterSample> findBySite_SiteId(String siteId); 
}