package za.co.tuks.amrdashboard.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "sites")
@Data
public class Site {

    @Id
    @Column(name = "site_id")
    private String siteId;

    private String locationName;
    private String riverName;
    private Double latitude;
    private Double longitude;

    // The import run that created this row (null for pre-existing data). Used for rollback.
    @Column(name = "import_id")
    private java.util.UUID importId;

    // One Site has many Water Samples
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private List<WaterSample> waterSamples;
}