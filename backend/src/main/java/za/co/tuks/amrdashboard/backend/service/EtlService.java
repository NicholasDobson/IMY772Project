package za.co.tuks.amrdashboard.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import za.co.tuks.amrdashboard.backend.model.*;
import za.co.tuks.amrdashboard.backend.repository.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlService {

    private final SiteRepository siteRepository;
    private final WaterSampleRepository waterSampleRepository;
    private final IsolateRepository isolateRepository;
    private final AmrSequenceRepository amrSequenceRepository;
    private final WgsMetricsRepository wgsMetricsRepository;
    private final ImportBatchRepository importBatchRepository;

    private final Map<String, java.util.UUID> mockUserDatabase = Map.of(
            "jane.doe@tuks.co.za", java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            "john.smith@tuks.co.za", java.util.UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
    );

    // If any exception happens inside this method, Spring reverts ALL database inserts
    // (including the ImportBatch row), so a failed upload leaves no trace.
    @Transactional(rollbackFor = Exception.class)
    public List<String> processBatch(MultipartFile epicollect, MultipartFile binaryInfo, MultipartFile amrFinder, MultipartFile starAmr) throws Exception {
        List<String> warnings = new ArrayList<>();

        String fileNames = Stream.of(epicollect, binaryInfo, amrFinder, starAmr)
                .filter(Objects::nonNull)
                .map(MultipartFile::getOriginalFilename)
                .collect(Collectors.joining(", "));
        UUID importId = createImportBatch("MULTI", fileNames);

        if (epicollect != null) warnings.addAll(parseEpicollect(extractData(epicollect), importId));
        if (binaryInfo != null) warnings.addAll(parseBinaryInfo(extractData(binaryInfo), importId));
        if (amrFinder != null) warnings.addAll(parseAmrFinder(extractData(amrFinder), importId));
        if (starAmr != null) warnings.addAll(parseStarAmr(extractData(starAmr), importId));

        finaliseImportBatch(importId);
        return warnings;
    }

    // --- Import tracking & rollback ---

    /** Creates an ImportBatch up-front so its generated id can stamp every row created in this run. */
    private UUID createImportBatch(String type, String fileNames) {
        ImportBatch batch = new ImportBatch();
        batch.setImportType(type);
        batch.setFileNames(fileNames);
        batch.setImportedAt(LocalDateTime.now());
        return importBatchRepository.save(batch).getImportId();
    }

    /** Records how many new rows this import created, for the Imports tab summary. */
    private void finaliseImportBatch(UUID importId) {
        ImportBatch batch = importBatchRepository.findById(importId).orElseThrow();
        batch.setSiteCount((int) siteRepository.countByImportId(importId));
        batch.setSampleCount((int) waterSampleRepository.countByImportId(importId));
        batch.setIsolateCount((int) isolateRepository.countByImportId(importId));
        batch.setSequenceCount((int) amrSequenceRepository.countByImportId(importId));
        batch.setWgsCount((int) wgsMetricsRepository.countByImportId(importId));
        importBatchRepository.save(batch);
    }

    @Transactional(readOnly = true)
    public List<ImportBatch> getImports() {
        return importBatchRepository.findAllByOrderByImportedAtDesc();
    }

    /**
     * Rolls back (deletes) every row created by the given import, then removes the import record.
     * Deletes are issued child-first (sequences/metrics -> isolates -> samples -> sites) so foreign
     * keys are never violated. Only rows this import *created* are removed; rows it merely updated
     * (pre-existing data) are left intact. The whole operation is atomic.
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollbackImport(UUID importId) {
        ImportBatch batch = importBatchRepository.findById(importId)
                .orElseThrow(() -> new IllegalArgumentException("Import not found: " + importId));

        amrSequenceRepository.deleteByImportId(importId);
        wgsMetricsRepository.deleteByImportId(importId);
        isolateRepository.deleteByImportId(importId);
        waterSampleRepository.deleteByImportId(importId);
        siteRepository.deleteByImportId(importId);

        importBatchRepository.delete(batch);
    }

    // --- Single File Pipeline ---

    /**
     * Processes the consolidated "Single File" upload. Every row carries a flattened
     * subset of the four multi-file sources (site, sample, isolate, AMR sequence and
     * a slice of WGS metrics). Fields not present in the single file are left null.
     *
     * Because the single file has no "Site ID" column, the Site is keyed on its
     * Location value (per the agreed mapping), so rows sharing a location merge into
     * a single site/map marker.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<String> processSingle(MultipartFile singleFile) throws Exception {
        List<String> warnings = new ArrayList<>();
        if (singleFile != null) {
            UUID importId = createImportBatch("SINGLE", singleFile.getOriginalFilename());
            warnings.addAll(parseSingleFile(extractData(singleFile), importId));
            finaliseImportBatch(importId);
        }
        return warnings;
    }

    private List<String> parseSingleFile(List<Map<String, String>> data, UUID importId) {
        List<String> warnings = new ArrayList<>();
        int rowNum = 1; // header is row 1, first data row is row 2

        for (Map<String, String> row : data) {
            rowNum++;

            String isolateId = row.get("Isolate ID");
            if (isolateId == null || isolateId.isBlank()) {
                throw new IllegalArgumentException("Single File Upload Failed: 'Isolate ID' is missing on row " + rowNum + ".");
            }

            // --- Site (keyed on Location) ---
            Site site = null;
            String location = row.get("Location");
            if (location != null && !location.isBlank()) {
                Optional<Site> existing = siteRepository.findById(location);
                site = existing.orElseGet(Site::new);
                if (existing.isEmpty()) site.setImportId(importId); // stamp only on create
                site.setSiteId(location);
                site.setLocationName(location);
                site.setRiverName(row.get("River Name"));

                Double lat = parseFlexibleDouble(row.get("Latitude"));
                Double lng = parseFlexibleDouble(row.get("Longitude"));
                if (lat != null) site.setLatitude(lat);
                if (lng != null) site.setLongitude(lng);

                // Reassign to the managed instance: save() merges assigned-ID entities and
                // returns a managed copy, leaving the original detached/transient.
                site = siteRepository.save(site);
            } else {
                warnings.add("Row " + rowNum + ": missing 'Location' - site/map data not recorded for Isolate " + isolateId + ".");
            }

            // --- Water Sample (single-file "Sample Name" holds the Sample ID) ---
            WaterSample sample = null;
            String sampleId = row.get("Sample Name");
            if (sampleId != null && !sampleId.isBlank()) {
                Optional<WaterSample> existing = waterSampleRepository.findById(sampleId);
                sample = existing.orElseGet(WaterSample::new);
                if (existing.isEmpty()) sample.setImportId(importId);
                sample.setSampleId(sampleId);
                if (site != null) sample.setSite(site);
                sample.setSampleName(sampleId);
                sample.setSampleAnalysisType(row.get("Sample Analysis Type"));

                String dateStr = row.get("Collection Date");
                if (dateStr != null && !dateStr.isBlank()) {
                    LocalDate parsed = parseFlexibleDate(dateStr);
                    if (parsed != null) sample.setCollectionDate(parsed);
                    else warnings.add("Row " + rowNum + ": could not parse date '" + dateStr + "' for Sample " + sampleId + ".");
                }

                sample.setWaterTemperature(parseFlexibleDouble(row.get("Temp of water")));
                sample.setPhLevel(parseFlexibleDouble(row.get("pH")));
                sample.setTds(parseFlexibleDouble(row.get("TDS (mg/L)")));
                sample.setDissolvedOxygen(parseFlexibleDouble(row.get("Dissolved Oxygen (mg/L)")));

                String email = row.getOrDefault("Collected By", "").trim().toLowerCase();
                if (!email.isBlank()) {
                    java.util.UUID userId = mockUserDatabase.get(email);
                    if (userId != null) sample.setCollectedByUserId(userId);
                    else warnings.add("Row " + rowNum + ": collector email not found in system: " + email + ".");
                }

                sample = waterSampleRepository.save(sample);
            }

            // --- Isolate ---
            Optional<Isolate> existingIsolate = isolateRepository.findById(isolateId);
            Isolate isolate = existingIsolate.orElseGet(Isolate::new);
            if (existingIsolate.isEmpty()) isolate.setImportId(importId);
            isolate.setIsolateId(isolateId);
            if (sample != null) isolate.setWaterSample(sample);
            isolate.setOrganismIdentity(row.get("Organism"));
            isolate.setSourceContext(row.get("Isolation source"));
            String virulence = row.get("Virulence Genes");
            if (virulence != null && !virulence.isBlank()) isolate.setVirulenceGenes(virulence);
            // Reassign to the managed instance so the AMR sequence / WGS metrics below
            // reference a persistent Isolate (not a transient one).
            isolate = isolateRepository.save(isolate);

            // --- AMR Sequence (one per row, only when a gene is present) ---
            String geneSymbol = row.get("AMR Resistance genes");
            if (geneSymbol != null && !geneSymbol.isBlank()) {
                AmrSequence seq = new AmrSequence();
                seq.setImportId(importId);
                seq.setIsolate(isolate);
                seq.setGeneSymbol(geneSymbol);
                seq.setSequenceName(row.get("Sequence Name"));
                seq.setElementType(row.get("Element type"));
                seq.setResistanceClass(row.get("Class"));
                seq.setResistanceSubclass(row.get("Subclass"));
                seq.setTargetLength(parseFlexibleInteger(row.get("Target length")));
                seq.setReferenceSequenceLength(parseFlexibleInteger(row.get("Reference sequence length")));
                seq.setIdentityPercentage(parseFlexibleDouble(row.get("% Identity to reference sequence")));
                seq.setCoveragePercentage(parseFlexibleDouble(row.get("% Coverage of reference sequence")));
                seq.setAlignmentLength(parseFlexibleInteger(row.get("Alignment Length")));
                seq.setAccessionClosestSequence(row.get("Accession of Closest Sequence"));
                amrSequenceRepository.save(seq);
            }

            // --- WGS Metrics (single file only carries Plasmid + SIR profile) ---
            String plasmid = row.get("Plasmid Replicons");
            String sir = row.get("Predicted SIR profile");
            if ((plasmid != null && !plasmid.isBlank()) || (sir != null && !sir.isBlank())) {
                Optional<WgsMetrics> existing = wgsMetricsRepository.findByIsolate_IsolateId(isolateId);
                WgsMetrics metrics = existing.orElseGet(WgsMetrics::new);
                if (existing.isEmpty()) metrics.setImportId(importId);
                metrics.setIsolate(isolate);
                if (plasmid != null && !plasmid.isBlank()) metrics.setPlasmid(plasmid);
                if (sir != null && !sir.isBlank()) metrics.setPredictedSirProfile(sir);
                wgsMetricsRepository.save(metrics);
            }
        }
        return warnings;
    }

    // --- Core Extraction Engine (Supports XLSX, CSV, TSV) ---

    private List<Map<String, String>> extractData(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        
        if (filename.endsWith(".csv") || filename.endsWith(".tsv")) {
            return extractDelimitedData(file, filename.endsWith(".tsv") ? '\t' : ',');
        } else if (filename.endsWith(".xlsx")) {
            return extractExcelData(file);
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + filename + ". Please upload .xlsx, .csv, or .tsv");
        }
    }

    private List<Map<String, String>> extractDelimitedData(MultipartFile file, char delimiter) throws Exception {
        List<Map<String, String>> records = new ArrayList<>();
        CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(delimiter).setHeader().setSkipHeaderRecord(true).build();
        
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, format)) {
            
            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> row = new HashMap<>();
                csvParser.getHeaderNames().forEach(header -> row.put(header.trim(), csvRecord.get(header).trim()));
                records.add(row);
            }
        }
        return records;
    }

    private List<Map<String, String>> extractExcelData(MultipartFile file) throws Exception {
        List<Map<String, String>> records = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new IllegalArgumentException("Excel file is missing a header row.");

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell).trim());
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, String> record = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    record.put(headers.get(j), getCellValueAsString(row.getCell(j)));
                }
                records.add(record);
            }
        }
        return records;
    }

    // --- Domain Parsers (Using Headers) ---

    private List<String> parseEpicollect(List<Map<String, String>> data, UUID importId) {
        List<String> warnings = new ArrayList<>();
        for (Map<String, String> row : data) {
            String siteId = row.get("Site ID");
            if (siteId == null || siteId.isBlank()) {
                throw new IllegalArgumentException("Epicollect Upload Failed: Critical column 'Site ID' is missing or empty.");
            }

            Optional<Site> existingSite = siteRepository.findById(siteId);
            Site site = existingSite.orElseGet(Site::new);
            if (existingSite.isEmpty()) site.setImportId(importId);
            site.setSiteId(siteId);
            site.setLocationName(row.get("Location Name"));
            site.setRiverName(row.get("River Name"));
            site.setLatitude(parseDouble(row.get("Lat")));
            site.setLongitude(parseDouble(row.get("Lng")));
            site = siteRepository.save(site);

            String sampleId = row.get("Sample ID");
            if (sampleId == null || sampleId.isBlank()) continue;

            Optional<WaterSample> existingSample = waterSampleRepository.findById(sampleId);
            WaterSample sample = existingSample.orElseGet(WaterSample::new);
            if (existingSample.isEmpty()) sample.setImportId(importId);
            sample.setSampleId(sampleId);
            sample.setSite(site);
            sample.setSampleName(row.get("Sample Name"));
            sample.setSampleAnalysisType(row.get("Analysis Type"));
            sample.setTripIdentifier(row.get("Trip ID"));

            String dateStr = row.get("Date");
            if (dateStr != null && !dateStr.isBlank()) {
                try {
                    sample.setCollectionDate(LocalDate.parse(dateStr));
                } catch (Exception e) {
                    warnings.add("Could not parse date '" + dateStr + "' for Sample ID " + sampleId);
                }
            } else {
                warnings.add("Missing collection date for Sample ID " + sampleId);
            }

            sample.setWaterTemperature(parseDouble(row.get("Temp")));
            sample.setPhLevel(parseDouble(row.get("pH")));
            sample.setTds(parseDouble(row.get("TDS")));
            sample.setEc(parseDouble(row.get("EC")));
            sample.setDissolvedOxygen(parseDouble(row.get("DO")));

            String email = row.getOrDefault("Collector Email", "").trim().toLowerCase();
            if (!email.isBlank()) {
                java.util.UUID userId = mockUserDatabase.get(email);
                if (userId != null) sample.setCollectedByUserId(userId);
                else warnings.add("Collector email not found in system: " + email);
            }

            waterSampleRepository.save(sample);
        }
        return warnings;
    }

    private List<String> parseBinaryInfo(List<Map<String, String>> data, UUID importId) {
        List<String> warnings = new ArrayList<>();
        for (Map<String, String> row : data) {
            String isolateId = row.get("Isolate ID");
            if (isolateId == null || isolateId.isBlank()) {
                throw new IllegalArgumentException("Binary Info Upload Failed: 'Isolate ID' is missing.");
            }

            String sampleId = row.get("Sample ID");
            WaterSample sample = null;
            if (sampleId != null && !sampleId.isBlank()) {
                sample = waterSampleRepository.findById(sampleId).orElseGet(() -> {
                    WaterSample stub = new WaterSample();
                    stub.setSampleId(sampleId);
                    stub.setImportId(importId);
                    return waterSampleRepository.save(stub);
                });
            }

            Optional<Isolate> existingIsolate = isolateRepository.findById(isolateId);
            Isolate isolate = existingIsolate.orElseGet(Isolate::new);
            if (existingIsolate.isEmpty()) isolate.setImportId(importId);
            isolate.setIsolateId(isolateId);
            isolate.setWaterSample(sample);
            isolate.setIsolateNumber(row.get("Isolate Number"));
            isolate.setOrganismIdentity(row.get("Organism"));
            isolate.setSourceContext(row.get("Context"));
            isolate.setArCode(row.get("AR Code"));
            isolate.setVirulenceGenes(row.get("Virulence Genes"));

            Map<String, Boolean> profile = new HashMap<>();
            // Safely check flags, defaulting to false if column is missing
            profile.put("Intl1", "1".equals(row.getOrDefault("Intl1", "0")));
            profile.put("Intl2", "1".equals(row.getOrDefault("Intl2", "0")));
            profile.put("Intl3", "1".equals(row.getOrDefault("Intl3", "0")));
            profile.put("TEM", "1".equals(row.getOrDefault("TEM", "0")));
            profile.put("SHV", "1".equals(row.getOrDefault("SHV", "0")));
            isolate.setBinaryTypingProfile(profile);

            String email = row.getOrDefault("Owner Email", "").trim().toLowerCase();
            if (!email.isBlank()) {
                java.util.UUID userId = mockUserDatabase.get(email);
                if (userId != null) isolate.setOwnerId(userId);
                else warnings.add("Owner email not found in system: " + email);
            }

            isolateRepository.save(isolate);
        }
        return warnings;
    }

    private List<String> parseAmrFinder(List<Map<String, String>> data, UUID importId) {
        List<String> warnings = new ArrayList<>();
        for (Map<String, String> row : data) {
            String isolateId = row.get("Isolate ID");
            if (isolateId == null || isolateId.isBlank()) {
                throw new IllegalArgumentException("AMR Finder Upload Failed: 'Isolate ID' is missing.");
            }

            Isolate isolate = isolateRepository.findById(isolateId).orElseGet(() -> {
                Isolate stub = new Isolate();
                stub.setIsolateId(isolateId);
                stub.setImportId(importId);
                return isolateRepository.save(stub);
            });

            AmrSequence seq = new AmrSequence();
            seq.setImportId(importId);
            seq.setIsolate(isolate);
            seq.setGeneSymbol(row.get("Gene Symbol"));
            seq.setSequenceName(row.get("Sequence Name"));
            seq.setElementType(row.get("Element Type"));
            seq.setResistanceClass(row.get("Class"));
            seq.setResistanceSubclass(row.get("Subclass"));
            
            seq.setTargetLength(parseInteger(row.get("Target Length")));
            seq.setReferenceSequenceLength(parseInteger(row.get("Reference Length")));
            seq.setIdentityPercentage(parseDouble(row.get("Identity %")));
            seq.setCoveragePercentage(parseDouble(row.get("Coverage %")));
            seq.setAlignmentLength(parseInteger(row.get("Alignment Length")));
            seq.setAccessionClosestSequence(row.get("Accession"));

            amrSequenceRepository.save(seq);
        }
        return warnings;
    }

    private List<String> parseStarAmr(List<Map<String, String>> data, UUID importId) {
        List<String> warnings = new ArrayList<>();
        for (Map<String, String> row : data) {
            String isolateId = row.get("Isolate ID");
            if (isolateId == null || isolateId.isBlank()) {
                throw new IllegalArgumentException("Star AMR Upload Failed: 'Isolate ID' is missing.");
            }

            Isolate isolate = isolateRepository.findById(isolateId).orElseGet(() -> {
                Isolate stub = new Isolate();
                stub.setIsolateId(isolateId);
                stub.setImportId(importId);
                return isolateRepository.save(stub);
            });

            Optional<WgsMetrics> existingMetrics = wgsMetricsRepository.findByIsolate_IsolateId(isolateId);
            WgsMetrics metrics = existingMetrics.orElseGet(WgsMetrics::new);
            if (existingMetrics.isEmpty()) metrics.setImportId(importId);
            metrics.setIsolate(isolate);
            metrics.setQualityStatus(row.get("Quality Status"));
            metrics.setGenotype(row.get("Genotype"));
            metrics.setPredictedPhenotype(row.get("Predicted Phenotype"));
            metrics.setPredictedSirProfile(row.get("SIR Profile"));
            metrics.setPlasmid(row.get("Plasmid"));
            metrics.setGenomeLength(parseInteger(row.get("Genome Length")));
            metrics.setN50Value(parseInteger(row.get("N50 Value")));

            wgsMetricsRepository.save(metrics);
        }
        return warnings;
    }

    // --- Utility Methods ---

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                // Instantly format dates so downstream parsers get standard strings
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
                }
                if (cell.getNumericCellValue() == Math.floor(cell.getNumericCellValue())) {
                    yield String.valueOf((long) cell.getNumericCellValue());
                }
                yield String.valueOf(cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private Double parseDouble(String val) {
        if (val == null || val.isBlank()) return null;
        try { return Double.parseDouble(val); } catch (NumberFormatException e) { return null; }
    }

    private Integer parseInteger(String val) {
        if (val == null || val.isBlank()) return null;
        try { return (int) Double.parseDouble(val); } catch (NumberFormatException e) { return null; }
    }

    // --- Single-file tolerant parsers (comma decimal separators, mixed date formats) ---

    /** Like parseDouble but tolerates comma decimal separators (e.g. "76,44"). */
    private Double parseFlexibleDouble(String val) {
        if (val == null || val.isBlank()) return null;
        try { return Double.parseDouble(val.trim().replace(",", ".")); } catch (NumberFormatException e) { return null; }
    }

    /** Like parseInteger but tolerates comma decimal separators. */
    private Integer parseFlexibleInteger(String val) {
        if (val == null || val.isBlank()) return null;
        try { return (int) Double.parseDouble(val.trim().replace(",", ".")); } catch (NumberFormatException e) { return null; }
    }

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    /** Parses dates in DD-MM-YYYY (single-file format) as well as ISO and a few common variants. */
    private LocalDate parseFlexibleDate(String dateStr) {
        String s = dateStr.trim();
        for (DateTimeFormatter f : DATE_FORMATS) {
            try { return LocalDate.parse(s, f); } catch (Exception ignored) { /* try next */ }
        }
        return null;
    }
}