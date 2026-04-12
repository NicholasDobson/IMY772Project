package za.co.tuks.amrdashboard.backend.dto;

// Lightweight object just for plotting points on Mapbox/Google Maps
public record SiteMarkerDTO(
    String siteId,
    String locationName,
    String riverName,
    Double latitude,
    Double longitude
) {}