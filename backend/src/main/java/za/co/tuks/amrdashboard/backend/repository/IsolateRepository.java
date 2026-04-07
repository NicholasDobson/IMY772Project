package za.co.tuks.amrdashboard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.tuks.amrdashboard.backend.model.Isolate;
import java.util.List;

@Repository
public interface IsolateRepository extends JpaRepository<Isolate, String> {
    List<Isolate> findByOrganismIdentity(String organismIdentity);
}