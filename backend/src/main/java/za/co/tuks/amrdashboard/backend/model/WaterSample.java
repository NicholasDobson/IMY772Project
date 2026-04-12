package za.co.tuks.amrdashboard.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "water_samples")
@Data
public class WaterSample {

    @Id
    @Column(name = "sample_id")
    private String sampleId;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private Site site;

    // AWS Cognito User ID
    @Column(name = "collected_by_user_id")
    private java.util.UUID collectedByUserId;

    private String tripIdentifier;
    private LocalDate collectionDate;
    
    // Environmental Metadata
    private Double waterTemperature;
    private Double phLevel;
    private Double tds;
    private Double ec;
    private Double dissolvedOxygen;

    @OneToMany(mappedBy = "waterSample", cascade = CascadeType.ALL)
    private List<Isolate> isolates;

    private String sampleName;
    private String sampleAnalysisType; // e.g., "Metagenomics", "WGS"
}