package za.co.tuks.amrdashboard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.tuks.amrdashboard.backend.model.Isolate;
import java.util.List;

@Repository
public interface IsolateRepository extends JpaRepository<Isolate, String> {
    List<Isolate> findByOrganismIdentity(String organismIdentity);

    //Fetch unique organisms for the dropdown filter
    @Query("SELECT DISTINCT i.organismIdentity FROM Isolate i WHERE i.organismIdentity IS NOT NULL")
    List<String> findDistinctOrganisms();
  
    List<Isolate> findBySiteId(String siteId);
}