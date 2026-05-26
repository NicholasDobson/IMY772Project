package za.co.tuks.amrdashboard.backend.dto;

import java.util.List;

public record FilterOptionsDTO(
    List<String> rivers,
    List<String> organisms,
    List<String> sirProfiles,
    List<String> trips
) {}
