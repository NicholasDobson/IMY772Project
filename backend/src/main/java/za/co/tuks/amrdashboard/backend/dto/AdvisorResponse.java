package za.co.tuks.amrdashboard.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
public class AdvisorResponse {
    private String reply;
    private boolean ok;
    private String error;
    private List<SourceReference> sources;

    public static AdvisorResponse ok(String reply, List<SourceReference> sources) {
        return new AdvisorResponse(reply, true, null, sources);
    }

    public static AdvisorResponse fail(String error) {
        return new AdvisorResponse(null, false, error, null);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SourceReference {
        private Long documentId;
        private String documentTitle;
        private String chunkPreview;
        private String pageNumbers;
        private double similarity;
    }
}
