package za.co.tuks.amrdashboard.backend.dto;

import lombok.Data;

@Data
public class AdvisorRequest {
    private String message;
    private String contextType;
    private String contextId;
}
