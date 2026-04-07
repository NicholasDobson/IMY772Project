package za.co.tuks.amrdashboard.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "amr_sequences")
@Data
public class AmrSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sequence_id")
    private UUID sequenceId;

    // Many AMR Sequences belong to one Isolate
    @ManyToOne
    @JoinColumn(name = "isolate_id", nullable = false)
    private Isolate isolate;

    private String geneSymbol;
    private String elementType;
    private String resistanceClass;
    private String resistanceSubclass;
    
    private Double identityPercentage;
    private Double coveragePercentage;
}