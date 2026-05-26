package za.co.tuks.amrdashboard.backend.dto;

public record SiteMarkerDTO(
    String siteId,
    String locationName,
    String riverName,
    Double latitude,
    Double longitude,
    String riskLevel,
    String riskColor,
    int resistantPercent,
    int totalWgs
) {}
