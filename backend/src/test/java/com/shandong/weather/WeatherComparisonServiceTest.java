package com.shandong.weather;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shandong.weather.common.BusinessException;
import com.shandong.weather.entity.ForecastModel;
import com.shandong.weather.entity.WeatherElement;
import com.shandong.weather.mapper.*;
import com.shandong.weather.service.WeatherQueryService;
import com.shandong.weather.vo.ComparisonVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static com.shandong.weather.WorkbenchTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WeatherComparisonServiceTest {
    private final ForecastRecordMapper records = mock(ForecastRecordMapper.class);
    private final CityMapper cities = mock(CityMapper.class);
    private final WeatherElementMapper elements = mock(WeatherElementMapper.class);
    private final ForecastModelMapper models = mock(ForecastModelMapper.class);
    private WeatherQueryService service;
    private ForecastModel ecmwf;
    private ForecastModel noaa;

    @BeforeEach
    void setUp() {
        service = new WeatherQueryService(records, CITIES, List.of("ECMWF", "NOAA"),
                List.of("T2M", "PRECIP"), cities, models, elements);
        when(cities.selectById(8L)).thenReturn(DictionaryTestData.cities().get(0));
        when(elements.selectById(71L)).thenReturn(DictionaryTestData.elements().get(0));
        ecmwf = DictionaryTestData.models().get(0);
        noaa = new ForecastModel();
        noaa.setId(59L); noaa.setModelCode("NOAA"); noaa.setModelName("NOAA database name");
        when(models.selectList(any())).thenReturn(List.of(noaa, ecmwf));
        when(records.selectComparisonData(eq(8L), eq(71L), eq(List.of(41L, 59L)), any(), any()))
                .thenReturn(List.of());
    }

    private ComparisonRecordRow point(String code, LocalDateTime time, String value) {
        return new ComparisonRecordRow(code, time, new BigDecimal(value));
    }

    private void rows(ComparisonRecordRow... values) {
        when(records.selectComparisonData(8L, 71L, List.of(41L, 59L), START, END)).thenReturn(List.of(values));
    }

    private ComparisonVO query() {
        return service.comparison(8L, 71L, START, END);
    }

    private int error(Long city, Long element, LocalDateTime start, LocalDateTime end) {
        return assertThrows(BusinessException.class, () -> service.comparison(city, element, start, end)).getCode();
    }

    private void orderedModels(ComparisonVO result) {
        assertThat(result.series()).extracting(ComparisonVO.ModelSeries::modelCode).containsExactly("ECMWF", "NOAA");
        assertThat(result.series()).extracting(ComparisonVO.ModelSeries::modelId).containsExactly(41L, 59L);
        assertThat(result.series()).extracting(ComparisonVO.ModelSeries::modelName)
                .containsExactly("数据库模型名称", "NOAA database name");
    }

    @Test
    void temperatureUsesActualMetadataAndSortsBothUnorderedSequences() {
        rows(point("NOAA", END, "20.50"), point("ECMWF", END, "21.10"),
                point("ECMWF", START, "19.00"), point("NOAA", START.plusHours(3), "19.60"),
                point("ECMWF", START.plusHours(3), "20.20"), point("NOAA", START, "18.40"));
        var result = query();
        assertThat(result.cityId()).isEqualTo(8L);
        assertThat(result.cityName()).isEqualTo("济南");
        assertThat(result.element()).isEqualTo(new ComparisonVO.ElementInfo(71L, "T2M", "2 米气温", "℃"));
        orderedModels(result);
        for (var series : result.series()) {
            assertThat(series.values()).extracting(ComparisonVO.SeriesPoint::forecastTime).containsExactly(
                    "2026-09-07 08:00:00", "2026-09-07 11:00:00", "2026-09-07 14:00:00");
        }
        assertThat(result.series().get(0).values()).extracting(ComparisonVO.SeriesPoint::value)
                .containsExactly(new BigDecimal("19.00"), new BigDecimal("20.20"), new BigDecimal("21.10"));
        assertThat(result.series().get(1).values()).extracting(ComparisonVO.SeriesPoint::value)
                .containsExactly(new BigDecimal("18.40"), new BigDecimal("19.60"), new BigDecimal("20.50"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void resolvesModelCodesRatherThanSeedIdsOrDictionaryOrder() {
        var result = query();
        orderedModels(result);
        var capture = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(models).selectList(capture.capture());
        assertThat(capture.getValue().getSqlSegment()).contains("model_code IN");
        assertThat(capture.getValue().getParamNameValuePairs().values()).containsExactlyInAnyOrder("ECMWF", "NOAA");
        verify(records).selectComparisonData(8L, 71L, List.of(41L, 59L), START, END);
    }

    @Test
    void precipitationRetainsTrueZeroAndBothModelValues() {
        var element = new WeatherElement();
        element.setId(83L); element.setElementCode("PRECIP"); element.setElementName("降水量"); element.setUnit("mm");
        when(elements.selectById(83L)).thenReturn(element);
        when(records.selectComparisonData(8L, 83L, List.of(41L, 59L), START, END)).thenReturn(List.of(
                point("ECMWF", START, "0.00"), point("ECMWF", START.plusHours(3), "0.40"), point("ECMWF", END, "0.20"),
                point("NOAA", START, "0.10"), point("NOAA", START.plusHours(3), "0.60"), point("NOAA", END, "0.30")));
        var result = service.comparison(8L, 83L, START, END);
        assertThat(result.element()).isEqualTo(new ComparisonVO.ElementInfo(83L, "PRECIP", "降水量", "mm"));
        orderedModels(result);
        assertThat(result.series().get(0).values()).extracting(ComparisonVO.SeriesPoint::value)
                .containsExactly(new BigDecimal("0.00"), new BigDecimal("0.40"), new BigDecimal("0.20"));
        assertThat(result.series().get(1).values()).extracting(ComparisonVO.SeriesPoint::value)
                .containsExactly(new BigDecimal("0.10"), new BigDecimal("0.60"), new BigDecimal("0.30"));
    }

    @Test
    void mismatchedTimesRemainIndependentWithoutZeroOrNullPoints() {
        rows(point("ECMWF", END, "21.10"), point("NOAA", END, "20.50"),
                point("ECMWF", START, "19.00"), point("NOAA", START.plusHours(3), "19.60"));
        var result = query(); orderedModels(result);
        assertThat(result.series().get(0).values()).extracting(ComparisonVO.SeriesPoint::forecastTime)
                .containsExactly("2026-09-07 08:00:00", "2026-09-07 14:00:00");
        assertThat(result.series().get(1).values()).extracting(ComparisonVO.SeriesPoint::forecastTime)
                .containsExactly("2026-09-07 11:00:00", "2026-09-07 14:00:00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ECMWF", "NOAA"})
    void singleEmptyModelIsStillPresent(String presentCode) {
        rows(point(presentCode, START, "0.00"));
        var result = query(); orderedModels(result);
        for (var series : result.series()) {
            if (presentCode.equals(series.modelCode())) {
                assertThat(series.values()).containsExactly(new ComparisonVO.SeriesPoint("2026-09-07 08:00:00", new BigDecimal("0.00")));
            } else {
                assertThat(series.values()).isEmpty();
            }
        }
    }

    @Test
    void emptyRetainsCityElementAndTwoEmptyModelSeries() {
        var result = query(); orderedModels(result);
        assertThat(result.cityName()).isEqualTo("济南");
        assertThat(result.element().elementCode()).isEqualTo("T2M");
        assertThat(result.series()).allSatisfy(s -> assertThat(s.values()).isEmpty());
    }

    @Test
    void invalidTimesAre400BeforeDatabaseAccess() {
        assertThat(error(8L, 71L, END, START)).isEqualTo(400);
        assertThat(error(8L, 71L, null, END)).isEqualTo(400);
        assertThat(error(8L, 71L, START, null)).isEqualTo(400);
        verifyNoInteractions(records, cities, elements, models);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 9007199254740992L})
    void invalidIdsAre400BeforeDatabaseAccess(long invalid) {
        assertThat(error(invalid, 71L, START, END)).isEqualTo(400);
        assertThat(error(8L, invalid, START, END)).isEqualTo(400);
        verifyNoInteractions(records, cities, elements, models);
    }

    @Test
    void nullIdsAre400AndMaximumSafeIdReachesExistenceCheck() {
        assertThat(error(null, 71L, START, END)).isEqualTo(400);
        assertThat(error(8L, null, START, END)).isEqualTo(400);
        assertThat(error(9007199254740991L, 71L, START, END)).isEqualTo(404);
        assertThat(error(8L, 9007199254740991L, START, END)).isEqualTo(404);
    }

    @Test
    void nonexistentCityIs404() {
        assertThat(error(999L, 71L, START, END)).isEqualTo(404);
        verifyNoInteractions(records, models);
    }

    @Test
    void nonexistentElementIs404() {
        assertThat(error(8L, 999L, START, END)).isEqualTo(404);
        verifyNoInteractions(records, models);
    }

    @Test
    void unsupportedCityIs400() {
        when(cities.selectById(8L)).thenReturn(DictionaryTestData.cities().get(1));
        assertThat(error(8L, 71L, START, END)).isEqualTo(400);
        verifyNoInteractions(records, models);
    }

    @Test
    void unsupportedElementIs400() {
        when(elements.selectById(71L)).thenReturn(DictionaryTestData.elements().get(1));
        assertThat(error(8L, 71L, START, END)).isEqualTo(400);
        verifyNoInteractions(records, models);
    }

    @Test
    void missingElementPrecedesUnsupportedCity() {
        when(cities.selectById(8L)).thenReturn(DictionaryTestData.cities().get(1));
        assertThat(error(8L, 999L, START, END)).isEqualTo(404);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ECMWF", "NOAA"})
    void missingEitherCoreModelIs500NotEmpty(String missing) {
        when(models.selectList(any())).thenReturn("ECMWF".equals(missing) ? List.of(noaa) : List.of(ecmwf));
        assertThat(error(8L, 71L, START, END)).isEqualTo(500);
        verifyNoInteractions(records);
    }

    @Test
    void qingdaoAndEqualTimeAreSupported() {
        var city = DictionaryTestData.cities().get(0);
        city.setCityCode("QINGDAO"); city.setCityName("青岛");
        when(cities.selectById(8L)).thenReturn(city);
        var result = service.comparison(8L, 71L, START, START);
        assertThat(result.cityName()).isEqualTo("青岛"); orderedModels(result);
        verify(records).selectComparisonData(8L, 71L, List.of(41L, 59L), START, START);
    }
}
