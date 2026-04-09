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
import java.util.HashMap;
import java.util.Map;
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

        // 1. Fetch or Create Site
        Site site = siteRepository.findById(siteId).orElse(new Site());
        site.setSiteId(siteId);
        site.setLocationName(getCellValueAsString(row.getCell(1)));
        site.setRiverName(getCellValueAsString(row.getCell(2)));
        site.setLatitude(getCellValueAsDouble(row.getCell(3)));
        site.setLongitude(getCellValueAsDouble(row.getCell(4)));
        siteRepository.save(site);

        // 2. Fetch or Create Water Sample
        String sampleId = getCellValueAsString(row.getCell(5));
        if (sampleId.isBlank()) return;

        // Assuming you temporarily changed WaterSample ID to String in your model
        WaterSample sample = waterSampleRepository.findById(sampleId).orElse(new WaterSample());
        sample.setSampleId(sampleId);
        sample.setSite(site);
        sample.setTripIdentifier(getCellValueAsString(row.getCell(6)));
        
        // Handle Date safely
        Cell dateCell = row.getCell(7);
        if (dateCell != null && dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
            LocalDate date = dateCell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            sample.setCollectionDate(date);
        }

        sample.setWaterTemperature(getCellValueAsDouble(row.getCell(8)));
        sample.setPhLevel(getCellValueAsDouble(row.getCell(9)));
        sample.setTds(getCellValueAsDouble(row.getCell(10)));
        sample.setEc(getCellValueAsDouble(row.getCell(11)));
        sample.setDissolvedOxygen(getCellValueAsDouble(row.getCell(12)));
        
        waterSampleRepository.save(sample);
    }

    private void parseBinaryInfo(Row row) {
        // Expected Columns:
        // 0: Sample ID (Link to WaterSample), 1: Isolate ID, 2: Isolate Number
        // 3: Organism, 4: Context, 5: AR Code, 6+: Binary Flags (Intl1, TEM, SHV, etc.)

        String sampleId = getCellValueAsString(row.getCell(0));
        if (sampleId.isBlank()) return;

        // STUB LOGIC: Find existing, or create and save an empty stub WaterSample
        WaterSample sample = waterSampleRepository.findById(sampleId).orElseGet(() -> {
            WaterSample stub = new WaterSample();
            stub.setSampleId(sampleId);
            return waterSampleRepository.save(stub); 
        });

        String isolateId = getCellValueAsString(row.getCell(1));
        if (isolateId.isBlank()) return;

        Isolate isolate = isolateRepository.findById(isolateId).orElse(new Isolate());
        isolate.setIsolateId(isolateId);
        isolate.setWaterSample(sample); // Relational Link to real or stubbed sample!
        isolate.setIsolateNumber(getCellValueAsString(row.getCell(2)));
        isolate.setOrganismIdentity(getCellValueAsString(row.getCell(3)));
        isolate.setSourceContext(getCellValueAsString(row.getCell(4)));
        isolate.setArCode(getCellValueAsString(row.getCell(5)));

        // Parse JSONB Binary Profile
        Map<String, Boolean> profile = new HashMap<>();
        profile.put("Intl1", "1".equals(getCellValueAsString(row.getCell(6))));
        profile.put("Intl2", "1".equals(getCellValueAsString(row.getCell(7))));
        profile.put("Intl3", "1".equals(getCellValueAsString(row.getCell(8))));
        profile.put("TEM", "1".equals(getCellValueAsString(row.getCell(9))));
        profile.put("SHV", "1".equals(getCellValueAsString(row.getCell(10))));
        
        isolate.setBinaryTypingProfile(profile);
        isolateRepository.save(isolate);
    }

    private void parseAmrFinder(Row row) {
        // Expected Columns:
        // 0: Isolate ID (Link to Isolate), 1: Gene Symbol, 2: Element Type
        // 3: Resistance Class, 4: Resistance Subclass, 5: Identity %, 6: Coverage %

        String isolateId = getCellValueAsString(row.getCell(0));
        if (isolateId.isBlank()) return;

        // STUB LOGIC: Find existing, or create and save an empty stub Isolate
        Isolate isolate = isolateRepository.findById(isolateId).orElseGet(() -> {
            Isolate stub = new Isolate();
            stub.setIsolateId(isolateId);
            return isolateRepository.save(stub);
        });

        AmrSequence seq = new AmrSequence();
        seq.setIsolate(isolate); // Relational Link to real or stubbed isolate!
        seq.setGeneSymbol(getCellValueAsString(row.getCell(1)));
        seq.setElementType(getCellValueAsString(row.getCell(2)));
        seq.setResistanceClass(getCellValueAsString(row.getCell(3)));
        seq.setResistanceSubclass(getCellValueAsString(row.getCell(4)));
        seq.setIdentityPercentage(getCellValueAsDouble(row.getCell(5)));
        seq.setCoveragePercentage(getCellValueAsDouble(row.getCell(6)));

        amrSequenceRepository.save(seq);
    }

    private void parseStarAmr(Row row) {
        // Expected Columns:
        // 0: Isolate ID, 1: Quality Status, 2: Genotype, 3: Predicted Phenotype
        // 4: Plasmid, 5: Genome Length, 6: N50 Value

        String isolateId = getCellValueAsString(row.getCell(0));
        if (isolateId.isBlank()) return;

        // STUB LOGIC: Find existing, or create and save an empty stub Isolate
        Isolate isolate = isolateRepository.findById(isolateId).orElseGet(() -> {
            Isolate stub = new Isolate();
            stub.setIsolateId(isolateId);
            return isolateRepository.save(stub);
        });

        WgsMetrics metrics = wgsMetricsRepository.findByIsolate_IsolateId(isolateId).orElse(new WgsMetrics());
        metrics.setIsolate(isolate); // Relational Link to real or stubbed isolate!
        metrics.setQualityStatus(getCellValueAsString(row.getCell(1)));
        metrics.setGenotype(getCellValueAsString(row.getCell(2)));
        metrics.setPredictedPhenotype(getCellValueAsString(row.getCell(3)));
        metrics.setPlasmid(getCellValueAsString(row.getCell(4)));
        
        Double genomeLength = getCellValueAsDouble(row.getCell(5));
        if (genomeLength != null) metrics.setGenomeLength(genomeLength.intValue());
        
        Double n50Value = getCellValueAsDouble(row.getCell(6));
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