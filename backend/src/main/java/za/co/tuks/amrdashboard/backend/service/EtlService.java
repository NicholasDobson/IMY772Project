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
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlService {

    private final SiteRepository siteRepository;
    private final WaterSampleRepository waterSampleRepository;
    private final IsolateRepository isolateRepository;
    private final AmrSequenceRepository amrSequenceRepository;
    private final WgsMetricsRepository wgsMetricsRepository;

    private final Map<String, java.util.UUID> mockUserDatabase = Map.of(
            "jane.doe@tuks.co.za", java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            "john.smith@tuks.co.za", java.util.UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
    );

    // If any exception happens inside this method, Spring reverts ALL database inserts
    @Transactional(rollbackFor = Exception.class)
    public List<String> processBatch(MultipartFile epicollect, MultipartFile binaryInfo, MultipartFile amrFinder, MultipartFile starAmr) throws Exception {
        List<String> warnings = new ArrayList<>();

        if (epicollect != null) warnings.addAll(parseEpicollect(extractData(epicollect)));
        if (binaryInfo != null) warnings.addAll(parseBinaryInfo(extractData(binaryInfo)));
        if (amrFinder != null) warnings.addAll(parseAmrFinder(extractData(amrFinder)));
        if (starAmr != null) warnings.addAll(parseStarAmr(extractData(starAmr)));

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

    private List<String> parseEpicollect(List<Map<String, String>> data) {
        List<String> warnings = new ArrayList<>();
        for (Map<String, String> row : data) {
            String siteId = row.get("Site ID");
            if (siteId == null || siteId.isBlank()) {
                throw new IllegalArgumentException("Epicollect Upload Failed: Critical column 'Site ID' is missing or empty.");
            }

            Site site = siteRepository.findById(siteId).orElse(new Site());
            site.setSiteId(siteId);
            site.setLocationName(row.get("Location Name"));
            site.setRiverName(row.get("River Name"));
            site.setLatitude(parseDouble(row.get("Lat")));
            site.setLongitude(parseDouble(row.get("Lng")));
            siteRepository.save(site);

            String sampleId = row.get("Sample ID");
            if (sampleId == null || sampleId.isBlank()) continue;

            WaterSample sample = waterSampleRepository.findById(sampleId).orElse(new WaterSample());
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

    private List<String> parseBinaryInfo(List<Map<String, String>> data) {
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
                    return waterSampleRepository.save(stub);
                });
            }

            Isolate isolate = isolateRepository.findById(isolateId).orElse(new Isolate());
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

    private List<String> parseAmrFinder(List<Map<String, String>> data) {
        List<String> warnings = new ArrayList<>();
        for (Map<String, String> row : data) {
            String isolateId = row.get("Isolate ID");
            if (isolateId == null || isolateId.isBlank()) {
                throw new IllegalArgumentException("AMR Finder Upload Failed: 'Isolate ID' is missing.");
            }

            Isolate isolate = isolateRepository.findById(isolateId).orElseGet(() -> {
                Isolate stub = new Isolate();
                stub.setIsolateId(isolateId);
                return isolateRepository.save(stub);
            });

            AmrSequence seq = new AmrSequence();
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

    private List<String> parseStarAmr(List<Map<String, String>> data) {
        List<String> warnings = new ArrayList<>();
        for (Map<String, String> row : data) {
            String isolateId = row.get("Isolate ID");
            if (isolateId == null || isolateId.isBlank()) {
                throw new IllegalArgumentException("Star AMR Upload Failed: 'Isolate ID' is missing.");
            }

            Isolate isolate = isolateRepository.findById(isolateId).orElseGet(() -> {
                Isolate stub = new Isolate();
                stub.setIsolateId(isolateId);
                return isolateRepository.save(stub);
            });

            WgsMetrics metrics = wgsMetricsRepository.findByIsolate_IsolateId(isolateId).orElse(new WgsMetrics());
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
}