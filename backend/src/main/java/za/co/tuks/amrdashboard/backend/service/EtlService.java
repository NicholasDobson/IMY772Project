package za.co.tuks.amrdashboard.backend.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import za.co.tuks.amrdashboard.backend.model.*;
import za.co.tuks.amrdashboard.backend.repository.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EtlService {

    private final IsolateRepository isolateRepository;
    private final AmrSequenceRepository amrSequenceRepository;
    private final WgsMetricsRepository wgsMetricsRepository;

    @Transactional
    public void processExcelFile(MultipartFile file, FileType fileType) throws Exception {
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0); // Assuming data is on the first sheet

            // Skip the header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                switch (fileType) {
                    case BINARY_INFO -> parseBinaryInfo(row);
                    case AMR_FINDER -> parseAmrFinder(row);
                    case STAR_AMR -> parseStarAmr(row);
                }
            }
        }
    }

    private void parseBinaryInfo(Row row) {
        // Col 0: University of Pretoria Culture number (Isolate ID)
        String isolateId = getCellValueAsString(row.getCell(0));
        if (isolateId == null || isolateId.isBlank()) return;

        // Fetch existing or create new Isolate
        Isolate isolate = isolateRepository.findById(isolateId).orElse(new Isolate());
        isolate.setIsolateId(isolateId);
        isolate.setIsolateNumber(getCellValueAsString(row.getCell(1)));
        isolate.setSourceContext(getCellValueAsString(row.getCell(4)));
        isolate.setArCode(getCellValueAsString(row.getCell(6)));
        isolate.setOrganismIdentity(getCellValueAsString(row.getCell(9)));

        // Handle the JSONB Binary Profile (Columns 11 to 25 are boolean flags)
        Map<String, Boolean> profile = new HashMap<>();
        profile.put("Intl1", "1".equals(getCellValueAsString(row.getCell(11))));
        profile.put("Intl2", "1".equals(getCellValueAsString(row.getCell(12))));
        profile.put("Intl3", "1".equals(getCellValueAsString(row.getCell(13))));
        profile.put("TEM", "1".equals(getCellValueAsString(row.getCell(17))));
        profile.put("SHV", "1".equals(getCellValueAsString(row.getCell(18))));
        // Add remaining genes...
        
        isolate.setBinaryTypingProfile(profile);
        isolateRepository.save(isolate);
    }

    private void parseAmrFinder(Row row) {
        // Col 0: SampleID (Mapping to Isolate for this context)
        String isolateId = getCellValueAsString(row.getCell(0));
        
        // Only save if the Isolate exists in the database
        Optional<Isolate> isolateOpt = isolateRepository.findById(isolateId);
        if (isolateOpt.isEmpty()) return; 

        AmrSequence seq = new AmrSequence();
        seq.setIsolate(isolateOpt.get());
        seq.setGeneSymbol(getCellValueAsString(row.getCell(2))); // Col 2: Gene symbol
        seq.setElementType(getCellValueAsString(row.getCell(5))); // Col 5: Element type
        seq.setResistanceClass(getCellValueAsString(row.getCell(7))); // Col 7: Class
        seq.setResistanceSubclass(getCellValueAsString(row.getCell(8))); // Col 8: Subclass
        
        Cell identityCell = row.getCell(13); // Col 13: % Identity
        if (identityCell != null && identityCell.getCellType() == CellType.NUMERIC) {
            seq.setIdentityPercentage(identityCell.getNumericCellValue());
        }

        amrSequenceRepository.save(seq);
    }

    private void parseStarAmr(Row row) {
        // Col 0: Isolate ID (e.g., UPMP-1126_assembly.fasta)
        String rawIsolateId = getCellValueAsString(row.getCell(0));
        // Strip the "_assembly.fasta" to match your DB IDs if necessary
        String isolateId = rawIsolateId != null ? rawIsolateId.replace("_assembly.fasta", "") : "";

        Optional<Isolate> isolateOpt = isolateRepository.findById(isolateId);
        if (isolateOpt.isEmpty()) return;

        WgsMetrics metrics = new WgsMetrics();
        metrics.setIsolate(isolateOpt.get());
        metrics.setQualityStatus(getCellValueAsString(row.getCell(1))); // Col 1: Quality Module
        metrics.setGenotype(getCellValueAsString(row.getCell(2))); // Col 2: Genotype
        metrics.setPredictedPhenotype(getCellValueAsString(row.getCell(3))); // Col 3: Predicted Phenotype
        metrics.setPlasmid(getCellValueAsString(row.getCell(5))); // Col 5: Plasmid
        
        Cell lengthCell = row.getCell(8); // Col 8: Genome Length
        if (lengthCell != null && lengthCell.getCellType() == CellType.NUMERIC) {
            metrics.setGenomeLength((int) lengthCell.getNumericCellValue());
        }

        wgsMetricsRepository.save(metrics);
    }

    // Utility method to safely read cell values
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}