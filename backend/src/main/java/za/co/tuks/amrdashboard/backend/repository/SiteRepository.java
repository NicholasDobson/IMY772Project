package za.co.tuks.amrdashboard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.tuks.amrdashboard.backend.model.Site;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<Site, String> {
    // This query traverses the relationships. 
    // If a filter is null, it ignores it. If it has a value, it applies it.
    @Query("SELECT DISTINCT s FROM Site s " +
           "LEFT JOIN s.waterSamples ws " +
           "LEFT JOIN ws.isolates i " +
           "LEFT JOIN i.wgsMetrics wgs " +
           "WHERE (:riverName IS NULL OR s.riverName = :riverName) " +
           "AND (:organism IS NULL OR i.organismIdentity = :organism) " +
           "AND (:sirProfile IS NULL OR wgs.predictedSirProfile = :sirProfile)")
    List<Site> findFilteredSites(
            @Param("riverName") String riverName,
            @Param("organism") String organism,
            @Param("sirProfile") String sirProfile
    );

    // Queries for the dropdown menus
    @Query("SELECT DISTINCT s.riverName FROM Site s WHERE s.riverName IS NOT NULL")
    List<String> findDistinctRivers();
}