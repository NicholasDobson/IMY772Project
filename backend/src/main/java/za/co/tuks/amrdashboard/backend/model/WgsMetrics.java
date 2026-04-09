package za.co.tuks.amrdashboard.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "wgs_metrics")
@Data
public class WgsMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wgs_id")
    private UUID wgsId;

    // One WGS Metric record belongs to one Isolate
    @OneToOne
    @JoinColumn(name = "isolate_id", nullable = false, unique = true)
    private Isolate isolate;

    private String qualityStatus;
    
    @Column(columnDefinition = "TEXT")
    private String predictedPhenotype;
    
    @Column(columnDefinition = "TEXT")
    private String genotype;
    
    @Column(columnDefinition = "TEXT")
    private String plasmid;
    
    private Integer genomeLength;
    private Integer n50Value;

    private String predictedSirProfile;
}