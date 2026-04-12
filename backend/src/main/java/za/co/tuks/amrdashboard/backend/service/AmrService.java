package za.co.tuks.amrdashboard.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.tuks.amrdashboard.backend.repository.AmrSequenceRepository;
import za.co.tuks.amrdashboard.backend.repository.IsolateRepository;
import za.co.tuks.amrdashboard.backend.model.Isolate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AmrService {

    @PersistenceContext
    private EntityManager em;

    private final IsolateRepository isolateRepository;

    // ── 1. Resistance Genes for an Organism ───────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getResistanceGenes(String organism, int limit) {
        List<Object[]> rows = em.createQuery(
            "SELECT a.geneSymbol, a.resistanceClass, a.resistanceSubclass, a.elementType, " +
            "COUNT(a), AVG(a.identityPercentage), AVG(a.coveragePercentage) " +
            "FROM AmrSequence a JOIN a.isolate i " +
            "WHERE i.organismIdentity = :organism " +
            "GROUP BY a.geneSymbol, a.resistanceClass, a.resistanceSubclass, a.elementType " +
            "ORDER BY COUNT(a) DESC"
        ).setParameter("organism", organism).setMaxResults(limit).getResultList();

        List<Map<String, Object>> genes = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> gene = new LinkedHashMap<>();
            gene.put("geneSymbol",        row[0]);
            gene.put("resistanceClass",   row[1]);
            gene.put("resistanceSubclass",row[2]);
            gene.put("elementType",       row[3]);
            gene.put("occurrenceCount",   ((Number) row[4]).longValue());
            gene.put("avgIdentityPct",    row[5] != null ? Math.round(((Number) row[5]).doubleValue() * 10.0) / 10.0 : 0.0);
            gene.put("avgCoveragePct",    row[6] != null ? Math.round(((Number) row[6]).doubleValue() * 10.0) / 10.0 : 0.0);
            genes.add(gene);
        }

        return Map.of("organism", organism, "genes", genes);
    }

    // ── 2. Resistance Profile from JSONB binary_typing_profile ────────────────

    public Map<String, Object> getResistanceProfile(String organism) {
        // Fetch all isolates for this organism that have a binary typing profile
        List<Isolate> isolates = isolateRepository.findByOrganismIdentity(organism);

        // Aggregate: for each antibiotic key, count how many isolates have it as true
        Map<String, long[]> tally = new LinkedHashMap<>();
        // tally[key] = [trueCount, totalCount]

        for (Isolate isolate : isolates) {
            Map<String, Boolean> profile = isolate.getBinaryTypingProfile();
            if (profile == null) continue;

            for (Map.Entry<String, Boolean> entry : profile.entrySet()) {
                tally.putIfAbsent(entry.getKey(), new long[]{0, 0});
                tally.get(entry.getKey())[1]++; // total
                if (Boolean.TRUE.equals(entry.getValue())) {
                    tally.get(entry.getKey())[0]++; // resistant count
                }
            }
        }

        // Find the most common ar_code for this organism
        String arCode = isolates.stream()
            .map(Isolate::getArCode)
            .filter(Objects::nonNull)
            .reduce((a, b) -> a) // just take first non-null
            .orElse("UNKNOWN");

        List<Map<String, Object>> profile = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : tally.entrySet()) {
            long resistant = entry.getValue()[0];
            long total = entry.getValue()[1];
            double resistanceRate = total == 0 ? 0.0 :
                Math.round((double) resistant / total * 1000.0) / 1000.0;

            // Classify: R > 0.7, I between 0.3-0.7, S < 0.3
            String level = resistanceRate > 0.7 ? "R" : resistanceRate >= 0.3 ? "I" : "S";

            Map<String, Object> entry2 = new LinkedHashMap<>();
            entry2.put("antibiotic", entry.getKey());
            entry2.put("resistanceRate", resistanceRate);
            entry2.put("level", level);
            profile.add(entry2);
        }

        // Sort by resistance rate descending
        profile.sort((a, b) -> Double.compare(
            (double) b.get("resistanceRate"),
            (double) a.get("resistanceRate")
        ));

        return Map.of(
            "organism", organism,
            "arCode", arCode,
            "resistanceProfile", profile
        );
    }
}
