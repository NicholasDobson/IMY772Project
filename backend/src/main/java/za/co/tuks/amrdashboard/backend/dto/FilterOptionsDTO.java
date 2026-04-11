package za.co.tuks.amrdashboard.backend.dto;

import java.util.List;

// Used to populate the frontend dropdown menus
public record FilterOptionsDTO(
    List<String> rivers,
    List<String> organisms,
    List<String> sirProfiles
) {}