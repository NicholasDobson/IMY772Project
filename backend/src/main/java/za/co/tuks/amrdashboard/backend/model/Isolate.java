package za.co.tuks.amrdashboard.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "isolates")
@Data
public class Isolate {

    @Id
    @Column(name = "isolate_id")
    private String isolateId;

    @ManyToOne
    @JoinColumn(name = "sample_id")
    private WaterSample waterSample;

    @Column(name = "owner_id")
    private java.util.UUID ownerId;

    private String isolateNumber;
    private String organismIdentity;
    private String sourceContext;
    private String arCode;

    @Column(columnDefinition = "TEXT")
    private String virulenceGenes;

    // Hibernate 6 mapping for PostgreSQL JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "binary_typing_profile", columnDefinition = "jsonb")
    private Map<String, Boolean> binaryTypingProfile; 

    @OneToMany(mappedBy = "isolate", cascade = CascadeType.ALL)
    private List<AmrSequence> amrSequences;

    @OneToOne(mappedBy = "isolate", cascade = CascadeType.ALL)
    private WgsMetrics wgsMetrics;
}