package za.co.tuks.amrdashboard.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records a single data-import run (one multi-file batch or one single-file upload).
 * Every domain row created during the run is stamped with this batch's importId, so the
 * whole import can later be rolled back (deleted) as a unit.
 */
@Entity
@Table(name = "import_batches")
@Data
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "import_id")
    private UUID importId;

    // "SINGLE" or "MULTI"
    @Column(name = "import_type")
    private String importType;

    @Column(name = "file_names", columnDefinition = "TEXT")
    private String fileNames;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    // Counts of newly-created rows attributable to this import (used for the UI summary).
    private Integer siteCount;
    private Integer sampleCount;
    private Integer isolateCount;
    private Integer sequenceCount;
    private Integer wgsCount;
}
