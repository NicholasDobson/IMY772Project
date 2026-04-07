package za.co.tuks.amrdashboard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.tuks.amrdashboard.backend.model.WgsMetrics;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WgsMetricsRepository extends JpaRepository<WgsMetrics, UUID> {
    // Find the WGS metrics for a specific isolate
    Optional<WgsMetrics> findByIsolate_IsolateId(String isolateId);
}