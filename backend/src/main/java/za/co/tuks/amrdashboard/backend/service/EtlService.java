package za.co.tuks.amrdashboard.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import za.co.tuks.amrdashboard.backend.model.*;
import za.co.tuks.amrdashboard.backend.repository.*;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlService {

    private final SiteRepository siteRepository;
    private final WaterSampleRepository waterSampleRepository;
    private final IsolateRepository isolateRepository;
    private final AmrSequenceRepository amrSequenceRepository;
    private final WgsMetricsRepository wgsMetricsRepository;

    // TODO: SPRINT 2 - Remove this mock map and replace with actual AWS Cognito / User Database lookup
    private final Map<String, java.util.UUID> mockUserDatabase = Map.of(
            "jane.doe@tuks.co.za", java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            "john.smith@tuks.co.za", java.util.UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
    );

    @Transactional
    public void processExcelFile(MultipartFile file, FileType fileType) throws Exception {
        log.info("Starting ETL process for file type: {}", fileType);
        
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0); // Assuming data is always on the first sheet

            // Skip the header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    switch (fileType) {
                        case EPICOLLECT -> parseEpicollect(row);
                        case BINARY_INFO -> parseBinaryInfo(row);
                        case AMR_FINDER -> parseAmrFinder(row);
                        case STAR_AMR -> parseStarAmr(row);
                    }
                } catch (Exception e) {
                    log.error("Error parsing row {}: {}", i, e.getMessage());
                    // Continue processing other rows even if one fails
                }
            }
        }
        log.info("Completed ETL process for file type: {}", fileType);
    }

    private void parseEpicollect(Row row) {
        // Expected Columns: 
        // 0: Site ID, 1: Location Name, 2: River Name, 3: Lat, 4: Lng
        // 5: Sample ID, 6: Trip ID, 7: Date, 8: Temp, 9: pH, 10: TDS, 11: EC, 12: DO

        String siteId = getCellValueAsString(row.getCell(0));
        if (siteId.isBlank()) return;

        Site site = siteRepository.findById(siteId).orElse(new Site());
        site.setSiteId(siteId);
        site.setLocationName(getCellValueAsString(row.getCell(1))); // Changed to geo_loc_name per video
        site.setRiverName(getCellValueAsString(row.getCell(2)));
        site.setLatitude(getCellValueAsDouble(row.getCell(3)));
        site.setLongitude(getCellValueAsDouble(row.getCell(4)));
        siteRepository.save(site);

        String sampleId = getCellValueAsString(row.getCell(5));
        if (sampleId.isBlank()) return;

        WaterSample sample = waterSampleRepository.findById(sampleId).orElse(new WaterSample());
        sample.setSampleId(sampleId);
        sample.setSite(site);
        sample.setSampleName(getCellValueAsString(row.getCell(6))); // NEW
        sample.setSampleAnalysisType(getCellValueAsString(row.getCell(7))); // NEW
        sample.setTripIdentifier(getCellValueAsString(row.getCell(8)));
        
        Cell dateCell = row.getCell(9);
        if (dateCell != null) {
            if (dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                // Handle properly formatted Excel dates
                sample.setCollectionDate(dateCell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            } else if (dateCell.getCellType() == CellType.STRING) {
                // Handle dates entered as plain text (e.g., "2025-05-10")
                try {
                    sample.setCollectionDate(java.time.LocalDate.parse(dateCell.getStringCellValue().trim()));
                } catch (java.time.format.DateTimeParseException e) {
                    // If it fails to parse, either leave it null or log it - we leave as null for now.
                    // log.warn("Invalid date format in text cell: {}", dateCell.getStringCellValue());
                }
            }
        }
        sample.setWaterTemperature(getCellValueAsDouble(row.getCell(10)));
        sample.setPhLevel(getCellValueAsDouble(row.getCell(11)));
        sample.setTds(getCellValueAsDouble(row.getCell(12)));
        sample.setEc(getCellValueAsDouble(row.getCell(13)));
        sample.setDissolvedOxygen(getCellValueAsDouble(row.getCell(14)));

        // Parse the Email for collected_by_user_id (Column 15)
        String collectorEmail = getCellValueAsString(row.getCell(15)).trim().toLowerCase();
        if (!collectorEmail.isBlank()) {
            java.util.UUID userId = mockUserDatabase.get(collectorEmail);
            if (userId != null) {
                sample.setCollectedByUserId(userId);
            } else {
                // Future enhancement: Throw an exception or log "User email not found in system"
                System.out.println("Warning: Collector email not found - " + collectorEmail);
            }
        }
        
        waterSampleRepository.save(sample);
    }

    private void parseBinaryInfo(Row row) {
        // Expected Columns:
        // 0: Sample ID (Link to WaterSample), 1: Isolate ID, 2: Isolate Number
        // 3: Organism, 4: Context, 5: AR Code, 6+: Binary Flags (Intl1, TEM, SHV, etc.)

        String sampleId = getCellValueAsString(row.getCell(0));
        if (sampleId.isBlank()) return;

        WaterSample sample = waterSampleRepository.findById(sampleId).orElseGet(() -> {
            WaterSample stub = new WaterSample();
            stub.setSampleId(sampleId);
            return waterSampleRepository.save(stub); 
        });

        String isolateId = getCellValueAsString(row.getCell(1));
        if (isolateId.isBlank()) return;

        Isolate isolate = isolateRepository.findById(isolateId).orElse(new Isolate());
        isolate.setIsolateId(isolateId);
        isolate.setWaterSample(sample);
        isolate.setIsolateNumber(getCellValueAsString(row.getCell(2)));
        isolate.setOrganismIdentity(getCellValueAsString(row.getCell(3)));
        isolate.setSourceContext(getCellValueAsString(row.getCell(4)));
        isolate.setArCode(getCellValueAsString(row.getCell(5)));
        isolate.setVirulenceGenes(getCellValueAsString(row.getCell(6))); // NEW

        Map<String, Boolean> profile = new HashMap<>();
        profile.put("Intl1", "1".equals(getCellValueAsString(row.getCell(7))));
        profile.put("Intl2", "1".equals(getCellValueAsString(row.getCell(8))));
        profile.put("Intl3", "1".equals(getCellValueAsString(row.getCell(9))));
        profile.put("TEM", "1".equals(getCellValueAsString(row.getCell(10))));
        profile.put("SHV", "1".equals(getCellValueAsString(row.getCell(11))));
        isolate.setBinaryTypingProfile(profile);

        // Parse the Email for owner_id (Column 12)
        String ownerEmail = getCellValueAsString(row.getCell(12)).trim().toLowerCase();
        if (!ownerEmail.isBlank()) {
            java.util.UUID ownerId = mockUserDatabase.get(ownerEmail);
            if (ownerId != null) {
                isolate.setOwnerId(ownerId);
            } else {
                System.out.println("Warning: Owner email not found - " + ownerEmail);
            }
        }
        
        isolateRepository.save(isolate);
    }

    private void parseAmrFinder(Row row) {
        // Expected Columns:
        // 0: Isolate ID (Link to Isolate), 1: Gene Symbol, 2: Element Type
        // 3: Resistance Class, 4: Resistance Subclass, 5: Identity %, 6: Coverage %

        String isolateId = getCellValueAsString(row.getCell(0));
        if (isolateId.isBlank()) return;

        Isolate isolate = isolateRepository.findById(isolateId).orElseGet(() -> {
            Isolate stub = new Isolate();
            stub.setIsolateId(isolateId);
            return isolateRepository.save(stub);
        });

        AmrSequence seq = new AmrSequence();
        seq.setIsolate(isolate); 
        seq.setGeneSymbol(getCellValueAsString(row.getCell(1)));
        seq.setSequenceName(getCellValueAsString(row.getCell(2))); // NEW
        seq.setElementType(getCellValueAsString(row.getCell(3)));
        seq.setResistanceClass(getCellValueAsString(row.getCell(4)));
        seq.setResistanceSubclass(getCellValueAsString(row.getCell(5)));
        
        Double targetLen = getCellValueAsDouble(row.getCell(6)); // NEW
        if (targetLen != null) seq.setTargetLength(targetLen.intValue());
        
        Double refLen = getCellValueAsDouble(row.getCell(7)); // NEW
        if (refLen != null) seq.setReferenceSequenceLength(refLen.intValue());

        seq.setIdentityPercentage(getCellValueAsDouble(row.getCell(8)));
        seq.setCoveragePercentage(getCellValueAsDouble(row.getCell(9)));
        
        Double alignLen = getCellValueAsDouble(row.getCell(10)); // NEW
        if (alignLen != null) seq.setAlignmentLength(alignLen.intValue());
        
        seq.setAccessionClosestSequence(getCellValueAsString(row.getCell(11))); // NEW

        amrSequenceRepository.save(seq);
    }

    private void parseStarAmr(Row row) {
        // Expected Columns:
        // 0: Isolate ID, 1: Quality Status, 2: Genotype, 3: Predicted Phenotype
        // 4: Plasmid, 5: Genome Length, 6: N50 Value

        String isolateId = getCellValueAsString(row.getCell(0));
        if (isolateId.isBlank()) return;

        Isolate isolate = isolateRepository.findById(isolateId).orElseGet(() -> {
            Isolate stub = new Isolate();
            stub.setIsolateId(isolateId);
            return isolateRepository.save(stub);
        });

        WgsMetrics metrics = wgsMetricsRepository.findByIsolate_IsolateId(isolateId).orElse(new WgsMetrics());
        metrics.setIsolate(isolate);
        metrics.setQualityStatus(getCellValueAsString(row.getCell(1)));
        metrics.setGenotype(getCellValueAsString(row.getCell(2)));
        metrics.setPredictedPhenotype(getCellValueAsString(row.getCell(3)));
        metrics.setPredictedSirProfile(getCellValueAsString(row.getCell(4))); // NEW
        metrics.setPlasmid(getCellValueAsString(row.getCell(5))); 
        
        Double genomeLength = getCellValueAsDouble(row.getCell(6));
        if (genomeLength != null) metrics.setGenomeLength(genomeLength.intValue());
        
        Double n50Value = getCellValueAsDouble(row.getCell(7));
        if (n50Value != null) metrics.setN50Value(n50Value.intValue());

        wgsMetricsRepository.save(metrics);
    }

    // --- Utility Methods ---

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                // If it's a whole number, prevent "1.0" format
                if (cell.getNumericCellValue() == Math.floor(cell.getNumericCellValue())) {
                    yield String.valueOf((long) cell.getNumericCellValue());
                }
                yield String.valueOf(cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}