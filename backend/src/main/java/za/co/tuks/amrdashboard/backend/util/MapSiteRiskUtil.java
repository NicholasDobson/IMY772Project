package za.co.tuks.amrdashboard.backend.util;

import za.co.tuks.amrdashboard.backend.model.AmrSequence;
import za.co.tuks.amrdashboard.backend.model.Isolate;
import za.co.tuks.amrdashboard.backend.model.Site;
import za.co.tuks.amrdashboard.backend.model.WaterSample;
import za.co.tuks.amrdashboard.backend.model.WgsMetrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Aligns map/site safety ratings with the logic used on the river detail view.
 */
public final class MapSiteRiskUtil {

    private MapSiteRiskUtil() {}

    public record SiteRisk(
            String level,
            String colorHex,
            String headline,
            String detail,
            int resistantCount,
            int totalWgs,
            int resistantPercent,
            Double latestPh,
            Double latestDo
    ) {}

    public static SiteRisk assess(Site site) {
        List<WgsMetrics> wgsList = new ArrayList<>();
        List<AmrSequence> amrList = new ArrayList<>();
        List<WaterSample> samples = site.getWaterSamples() != null ? site.getWaterSamples() : List.of();

        for (WaterSample sample : samples) {
            if (sample.getIsolates() == null) continue;
            for (Isolate isolate : sample.getIsolates()) {
                if (isolate.getWgsMetrics() != null) {
                    wgsList.add(isolate.getWgsMetrics());
                }
                if (isolate.getAmrSequences() != null) {
                    amrList.addAll(isolate.getAmrSequences());
                }
            }
        }

        boolean hasResistant = wgsList.stream()
                .anyMatch(w -> "Resistant".equalsIgnoreCase(w.getPredictedSirProfile()));
        boolean hasIntermediate = wgsList.stream()
                .anyMatch(w -> "Intermediate".equalsIgnoreCase(w.getPredictedSirProfile()));

        long resistantCount = wgsList.stream()
                .filter(w -> "Resistant".equalsIgnoreCase(w.getPredictedSirProfile()))
                .count();
        int totalWgs = wgsList.size();
        int resistantPercent = totalWgs == 0 ? 0 : (int) Math.round((double) resistantCount / totalWgs * 100);

        Set<String> resistanceClasses = new HashSet<>();
        for (AmrSequence seq : amrList) {
            if ("AMR".equalsIgnoreCase(seq.getElementType()) && seq.getResistanceClass() != null) {
                resistanceClasses.add(seq.getResistanceClass());
            }
        }
        boolean mdr = resistanceClasses.size() >= 2;

        List<Double> doValues = samples.stream()
                .map(WaterSample::getDissolvedOxygen)
                .filter(v -> v != null)
                .toList();
        Double latestDo = doValues.isEmpty() ? null : doValues.get(doValues.size() - 1);
        boolean lowO2 = latestDo != null && latestDo < 4;

        Double latestPh = samples.stream()
                .filter(s -> s.getPhLevel() != null)
                .max(Comparator.comparing(WaterSample::getCollectionDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(WaterSample::getPhLevel)
                .orElse(samples.stream()
                        .map(WaterSample::getPhLevel)
                        .filter(v -> v != null)
                        .reduce((a, b) -> b)
                        .orElse(null));

        if (hasResistant && mdr) {
            return new SiteRisk(
                    "HIGH RISK", "#DC2626",
                    "Not recommended for recreational water contact.",
                    "Multidrug-resistant bacteria detected (" + resistanceClasses.size()
                            + " resistance classes). Avoid contact.",
                    (int) resistantCount, totalWgs, resistantPercent, latestPh, latestDo
            );
        }
        if (hasResistant || lowO2) {
            String detail = lowO2
                    ? "Low dissolved oxygen (" + latestDo + " mg/L) detected."
                    + (hasResistant ? " Resistant bacteria also found." : "")
                    : "Antibiotic-resistant bacteria detected. Avoid swallowing water.";
            return new SiteRisk(
                    "CAUTION", "#D97706",
                    "Exercise caution near this water.",
                    detail,
                    (int) resistantCount, totalWgs, resistantPercent, latestPh, latestDo
            );
        }
        if (hasIntermediate) {
            return new SiteRisk(
                    "LOW ADVISORY", "#2563EB",
                    "Generally safe, with minor advisory.",
                    "Bacteria with reduced antibiotic susceptibility detected.",
                    (int) resistantCount, totalWgs, resistantPercent, latestPh, latestDo
            );
        }
        return new SiteRisk(
                "LOW RISK", "#059669",
                "No significant AMR threat detected at this site.",
                "Standard hygiene practices still recommended.",
                (int) resistantCount, totalWgs, resistantPercent, latestPh, latestDo
        );
    }
}
