package za.co.tuks.amrdashboard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.tuks.amrdashboard.backend.model.Site;

import java.util.List;
import java.util.UUID;

@Repository
public interface SiteRepository extends JpaRepository<Site, String> {
    // This query traverses the relationships. 
    // If a filter is null, it ignores it. If it has a value, it applies it.
    @Query("SELECT DISTINCT s FROM Site s " +
           "LEFT JOIN s.waterSamples ws " +
           "LEFT JOIN ws.isolates i " +
           "LEFT JOIN i.wgsMetrics wgs " +
           "WHERE (:riverNames IS NULL OR s.riverName IN :riverNames) " +
           "AND (:organisms IS NULL OR i.organismIdentity IN :organisms) " +
           "AND (:sirProfiles IS NULL OR wgs.predictedSirProfile IN :sirProfiles) " +
           "AND (:tripIds IS NULL OR ws.tripIdentifier IN :tripIds)")
    List<Site> findFilteredSites(
            @Param("riverNames") List<String> riverNames,
            @Param("organisms") List<String> organisms,
            @Param("sirProfiles") List<String> sirProfiles,
            @Param("tripIds") List<String> tripIds
    );

    // Queries for the dropdown menus
    @Query("SELECT DISTINCT s.riverName FROM Site s WHERE s.riverName IS NOT NULL")
    List<String> findDistinctRivers();

    long countByImportId(UUID importId);

    @Modifying
    @Query("DELETE FROM Site s WHERE s.importId = :importId")
    void deleteByImportId(@Param("importId") UUID importId);
}