package za.co.tuks.amrdashboard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.tuks.amrdashboard.backend.model.Isolate;
import java.util.List;
import java.util.UUID;

@Repository
public interface IsolateRepository extends JpaRepository<Isolate, String> {
    List<Isolate> findByOrganismIdentity(String organismIdentity);

    //Fetch unique organisms for the dropdown filter
    @Query("SELECT DISTINCT i.organismIdentity FROM Isolate i WHERE i.organismIdentity IS NOT NULL")
    List<String> findDistinctOrganisms();

    List<Isolate> findByWaterSample_Site_SiteId(String siteId);

    List<Isolate> findByWaterSample_SampleIdIn(List<String> sampleIds);

    long countByImportId(UUID importId);

    @Modifying
    @Query("DELETE FROM Isolate i WHERE i.importId = :importId")
    void deleteByImportId(@Param("importId") UUID importId);
}