package za.co.tuks.amrdashboard.backend.dto;

import java.util.List;

public record SiteSummaryDTO(
    String siteId,
    String locationName,
    String riverName,
    int totalWaterSamples,
    List<String> detectedOrganisms,
    String lastSampledDate,
    String riskLevel,
    String riskColor,
    String safetyHeadline,
    String safetyDetail,
    int resistantPercent,
    int resistantCount,
    int totalWgs,
    Double latestPh,
    Double latestDissolvedOxygen
) {}
