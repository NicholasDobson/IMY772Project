package za.co.tuks.amrdashboard.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.tuks.amrdashboard.backend.dto.FilterOptionsDTO;
import za.co.tuks.amrdashboard.backend.repository.IsolateRepository;
import za.co.tuks.amrdashboard.backend.repository.SiteRepository;
import za.co.tuks.amrdashboard.backend.repository.WaterSampleRepository;
import za.co.tuks.amrdashboard.backend.repository.WgsMetricsRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MapServiceTest {

    @Mock
    private SiteRepository siteRepository;
    @Mock
    private IsolateRepository isolateRepository;
    @Mock
    private WgsMetricsRepository wgsMetricsRepository;

    @Mock
    private WaterSampleRepository waterSampleRepository;

    @InjectMocks
    private MapService mapService;

    @Test
    void shouldGetFilterOptions() {
        // Arrange (Tell the fake repositories what to return)
        when(siteRepository.findDistinctRivers()).thenReturn(List.of("Apies River"));
        when(isolateRepository.findDistinctOrganisms()).thenReturn(List.of("E. coli"));
        when(wgsMetricsRepository.findDistinctSirProfiles()).thenReturn(List.of("Resistant"));
        when(waterSampleRepository.findDistinctTripIdentifiers()).thenReturn(List.of("Trip 1"));

        // Act
        FilterOptionsDTO options = mapService.getFilterOptions();

        // Assert
        assertEquals(1, options.rivers().size());
        assertEquals("Apies River", options.rivers().get(0));
        assertEquals("E. coli", options.organisms().get(0));
        assertEquals("Resistant", options.sirProfiles().get(0));
    }
}