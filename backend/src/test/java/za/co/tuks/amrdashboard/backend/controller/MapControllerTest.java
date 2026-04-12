package za.co.tuks.amrdashboard.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.tuks.amrdashboard.backend.dto.FilterOptionsDTO;
import za.co.tuks.amrdashboard.backend.service.MapService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MapController.class)
class MapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean // Mocks the service layer so the controller has something to call
    private MapService mapService;

    @Test
    void shouldReturnFilterOptions() throws Exception {
        // Arrange
        FilterOptionsDTO mockOptions = new FilterOptionsDTO(
                List.of("Apies River"), List.of("E. coli"), List.of("Resistant")
        );
        when(mapService.getFilterOptions()).thenReturn(mockOptions);

        // Act & Assert
        mockMvc.perform(get("/api/v1/map/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rivers[0]").value("Apies River"))
                .andExpect(jsonPath("$.organisms[0]").value("E. coli"));
    }
}