package com.shandong.weather;

import com.shandong.weather.common.BusinessException;
import com.shandong.weather.mapper.ForecastRecordMapper;
import com.shandong.weather.mapper.WorkbenchElementRow;
import com.shandong.weather.mapper.WorkbenchModelRow;
import com.shandong.weather.service.WeatherQueryService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.shandong.weather.WorkbenchTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class WeatherQueryServiceTest {
    private ForecastRecordMapper mapper;
    private WeatherQueryService service;

    @BeforeEach
    void setUp() {
        mapper = mock(ForecastRecordMapper.class);
        service = new WeatherQueryService(mapper, CITIES, List.of("ECMWF", "NOAA"), List.of("T2M", "PRECIP"),
                mock(com.shandong.weather.mapper.CityMapper.class),
                mock(com.shandong.weather.mapper.ForecastModelMapper.class),
                mock(com.shandong.weather.mapper.WeatherElementMapper.class));
    }

    private void dictionaries() {
        when(mapper.selectWorkbenchModel(41L)).thenReturn(MODEL);
        when(mapper.selectWorkbenchElement(71L)).thenReturn(ELEMENT);
    }

    @Test
    void assemblesDatabaseMetadataAndPreservesRecordOrderAndDecimalValues() {
        dictionaries();
        when(mapper.selectWorkbenchData(41L, 71L, START, END, CITIES)).thenReturn(List.of(
                row(8, "济南", START, "19.00"), row(9, "青岛", START, "19.70"),
                row(8, "济南", END, "21.10")));
        var result = service.workbench(41L, 71L, START, END);
        assertThat(result.model().id()).isEqualTo(41L);
        assertThat(result.model().modelName()).isEqualTo("Database model name");
        assertThat(result.element().unit()).isEqualTo("℃");
        assertThat(result.records()).extracting(r -> r.cityId()).containsExactly(8L, 9L, 8L);
        assertThat(result.records().get(0).value()).isEqualByComparingTo("19.00");
        assertThat(result.times()).containsExactly("2026-09-07 08:00:00", "2026-09-07 14:00:00");
    }

    @Test
    void deduplicatesAndSortsTimelineEvenIfMapperRowsAreOutOfOrder() {
        dictionaries();
        when(mapper.selectWorkbenchData(41L, 71L, START, END, CITIES)).thenReturn(List.of(
                row(8, "济南", END, "21.10"), row(8, "济南", START, "19.00"),
                row(9, "青岛", START, "19.70")));
        assertThat(service.workbench(41L, 71L, START, END).times())
                .containsExactly("2026-09-07 08:00:00", "2026-09-07 14:00:00");
    }

    @Test
    void emptyRangePreservesDictionaryMetadataAndReturnsEmptyArrays() {
        dictionaries();
        when(mapper.selectWorkbenchData(41L, 71L, START, END, CITIES)).thenReturn(List.of());
        var result = service.workbench(41L, 71L, START, END);
        assertThat(result.model().modelCode()).isEqualTo("ECMWF");
        assertThat(result.element().elementCode()).isEqualTo("T2M");
        assertThat(result.records()).isEmpty();
        assertThat(result.times()).isEmpty();
    }

    @Test
    void reversedRangeIsRejectedBeforeDatabaseAccess() {
        assertThat(assertThrows(BusinessException.class,
                () -> service.workbench(41L, 71L, END, START)).getCode()).isEqualTo(400);
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 9007199254740992L})
    void rejectsOutOfRangeIdsBeforeDatabaseAccess(long invalid) {
        assertThat(assertThrows(BusinessException.class,
                () -> service.workbench(invalid, 71L, START, END)).getCode()).isEqualTo(400);
        assertThat(assertThrows(BusinessException.class,
                () -> service.workbench(41L, invalid, START, END)).getCode()).isEqualTo(400);
        verifyNoInteractions(mapper);
    }

    @Test
    void missingModelReturns404() {
        assertThat(assertThrows(BusinessException.class,
                () -> service.workbench(41L, 71L, START, END)).getCode()).isEqualTo(404);
    }

    @Test
    void missingElementReturns404() {
        when(mapper.selectWorkbenchModel(41L)).thenReturn(MODEL);
        assertThat(assertThrows(BusinessException.class,
                () -> service.workbench(41L, 71L, START, END)).getCode()).isEqualTo(404);
    }

    @Test
    void unsupportedModelReturns400AfterBothExistenceChecks() {
        dictionaries();
        when(mapper.selectWorkbenchModel(41L)).thenReturn(new WorkbenchModelRow(41L, "OTHER", "Other"));
        assertThat(assertThrows(BusinessException.class,
                () -> service.workbench(41L, 71L, START, END)).getCode()).isEqualTo(400);
        verify(mapper, never()).selectWorkbenchData(anyLong(), anyLong(), any(), any(), anyList());
    }

    @Test
    void nonexistentElementTakesPrecedenceOverUnsupportedModel() {
        when(mapper.selectWorkbenchModel(41L)).thenReturn(new WorkbenchModelRow(41L, "OTHER", "Other"));
        assertThat(assertThrows(BusinessException.class,
                () -> service.workbench(41L, 71L, START, END)).getCode()).isEqualTo(404);
    }

    @Test
    void unsupportedElementReturns400() {
        dictionaries();
        when(mapper.selectWorkbenchElement(71L)).thenReturn(new WorkbenchElementRow(71L, "WIND", "Wind", "m/s"));
        assertThat(assertThrows(BusinessException.class,
                () -> service.workbench(41L, 71L, START, END)).getCode()).isEqualTo(400);
    }

    @Test
    void equalEndpointsAreAllowedAndPassedUnchanged() {
        dictionaries();
        when(mapper.selectWorkbenchData(41L, 71L, START, START, CITIES))
                .thenReturn(List.of(row(8, "济南", START, "19.00")));
        assertThat(service.workbench(41L, 71L, START, START).records()).hasSize(1);
    }

    @Test
    void maximumSafeIdsAreNotRejectedAsInvalid() {
        assertThat(assertThrows(BusinessException.class,
                () -> service.workbench(9007199254740991L, 9007199254740991L, START, END)).getCode())
                .isEqualTo(404);
    }
}
