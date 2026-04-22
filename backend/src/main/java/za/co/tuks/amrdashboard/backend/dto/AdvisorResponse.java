package za.co.tuks.amrdashboard.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdvisorResponse {
    private String reply;
    private boolean ok;
    private String error;

    public static AdvisorResponse ok(String reply) {
        return new AdvisorResponse(reply, true, null);
    }

    public static AdvisorResponse fail(String error) {
        return new AdvisorResponse(null, false, error);
    }
}
