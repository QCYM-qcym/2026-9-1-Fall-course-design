package com.shandong.weather;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shandong.weather.common.BusinessException;
import com.shandong.weather.entity.City;
import com.shandong.weather.entity.ForecastModel;
import com.shandong.weather.entity.WeatherElement;
import com.shandong.weather.mapper.*;
import com.shandong.weather.service.WeatherQueryService;
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

class WeatherTrendServiceTest {
    private final ForecastRecordMapper records = mock(ForecastRecordMapper.class);
    private final CityMapper cities = mock(CityMapper.class);
    private final ForecastModelMapper models = mock(ForecastModelMapper.class);
    private final WeatherElementMapper elements = mock(WeatherElementMapper.class);
    private WeatherQueryService service;
    private City city;
    private ForecastModel model;
    private WeatherElement temperature;
    private WeatherElement precipitation;

    @BeforeEach
    void setUp() {
        service = new WeatherQueryService(records, CITIES, List.of("ECMWF", "NOAA"),
                List.of("T2M", "PRECIP"), cities, models, elements);
        city = DictionaryTestData.cities().get(0);
        model = DictionaryTestData.models().get(0);
        temperature = DictionaryTestData.elements().get(0);
        precipitation = new WeatherElement();
        precipitation.setId(83L); precipitation.setElementCode("PRECIP");
        precipitation.setElementName("降水量"); precipitation.setUnit("mm");
        when(cities.selectById(8L)).thenReturn(city);
        when(models.selectById(41L)).thenReturn(model);
        when(elements.selectList(any())).thenReturn(List.of(precipitation, temperature));
        when(records.selectTrendData(eq(8L), eq(41L), eq(List.of(71L, 83L)), any(), any()))
                .thenReturn(List.of());
    }

    private TrendRecordRow point(String code, LocalDateTime time, String value) {
        return new TrendRecordRow(code, time, new BigDecimal(value));
    }

    private void rows(TrendRecordRow... rows) {
        when(records.selectTrendData(8L, 41L, List.of(71L, 83L), START, END)).thenReturn(List.of(rows));
    }

    private int error(Long cityId, Long modelId, LocalDateTime start, LocalDateTime end) {
        return assertThrows(BusinessException.class, () -> service.trend(cityId, modelId, start, end)).getCode();
    }

    @Test
    void groupsUnsortedRowsAndComputesStatisticsWithoutSeedIds() {
        rows(point("PRECIP", END, "0.20"), point("T2M", END, "21.10"),
                point("T2M", START, "19.00"), point("PRECIP", START, "0.00"),
                point("PRECIP", START.plusHours(3), "0.40"), point("T2M", START.plusHours(3), "20.20"));
        var result = service.trend(8L, 41L, START, END);
        assertThat(result.cityId()).isEqualTo(8L);
        assertThat(result.cityName()).isEqualTo(city.getCityName());
        assertThat(result.modelId()).isEqualTo(41L);
        assertThat(result.modelName()).isEqualTo(model.getModelName());
        assertThat(result.temperature()).extracting(p -> p.forecastTime()).containsExactly(
                "2026-09-07 08:00:00", "2026-09-07 11:00:00", "2026-09-07 14:00:00");
        assertThat(result.precipitation()).extracting(p -> p.forecastTime()).containsExactly(
                "2026-09-07 08:00:00", "2026-09-07 11:00:00", "2026-09-07 14:00:00");
        assertThat(result.temperature()).extracting(p -> p.value()).containsExactly(
                new BigDecimal("19.00"), new BigDecimal("20.20"), new BigDecimal("21.10"));
        assertThat(result.statistics().temperatureMax()).isEqualByComparingTo("21.10");
        assertThat(result.statistics().temperatureMin()).isEqualByComparingTo("19.00");
        assertThat(result.statistics().temperatureAvg()).isEqualTo(new BigDecimal("20.10"));
        assertThat(result.statistics().precipitationTotal()).isEqualByComparingTo("0.60");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void resolvesCoreElementsByCodesNotIdsOrDictionaryOrder() {
        service.trend(8L, 41L, START, END);
        ArgumentCaptor<Wrapper<WeatherElement>> query = ArgumentCaptor.forClass((Class) Wrapper.class);
        verify(elements).selectList(query.capture());
        assertThat(query.getValue().getSqlSegment()).contains("element_code IN");
        var wrapper = (com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<WeatherElement>) query.getValue();
        assertThat(wrapper.getParamNameValuePairs().values()).containsExactlyInAnyOrder("T2M", "PRECIP");
        verify(records).selectTrendData(8L, 41L, List.of(71L, 83L), START, END);
    }

    @Test
    void roundsNonTerminatingAverageHalfUpToTwoPlaces() {
        rows(point("T2M", START, "19.00"), point("T2M", START.plusHours(3), "20.00"), point("T2M", END, "20.01"));
        assertThat(service.trend(8L, 41L, START, END).statistics().temperatureAvg()).isEqualTo(new BigDecimal("19.67"));
    }

    @Test
    void roundsNegativeHalfTieAwayFromZero() {
        rows(point("T2M", START, "-1.00"), point("T2M", END, "-1.01"));
        assertThat(service.trend(8L, 41L, START, END).statistics().temperatureAvg()).isEqualTo(new BigDecimal("-1.01"));
    }

    @Test
    void emptyPreservesMetadataAndFourNullStatistics() {
        var result = service.trend(8L, 41L, START, END);
        assertThat(result.cityName()).isEqualTo("济南");
        assertThat(result.modelName()).isEqualTo(model.getModelName());
        assertThat(result.temperature()).isEmpty(); assertThat(result.precipitation()).isEmpty();
        assertThat(result.statistics().temperatureMax()).isNull();
        assertThat(result.statistics().temperatureMin()).isNull();
        assertThat(result.statistics().temperatureAvg()).isNull();
        assertThat(result.statistics().precipitationTotal()).isNull();
    }

    @Test
    void temperatureOnlyDoesNotInventPrecipitationOrMissingMiddlePoint() {
        rows(point("T2M", START, "19.00"), point("T2M", END, "21.10"));
        var result = service.trend(8L, 41L, START, END);
        assertThat(result.temperature()).hasSize(2); assertThat(result.precipitation()).isEmpty();
        assertThat(result.statistics().temperatureAvg()).isEqualTo(new BigDecimal("20.05"));
        assertThat(result.statistics().precipitationTotal()).isNull();
    }

    @Test
    void precipitationOnlyKeepsMissingTemperatureStatisticsNull() {
        rows(point("PRECIP", START, "0.00"), point("PRECIP", END, "0.20"));
        var result = service.trend(8L, 41L, START, END);
        assertThat(result.temperature()).isEmpty(); assertThat(result.precipitation()).hasSize(2);
        assertThat(result.statistics().temperatureMax()).isNull();
        assertThat(result.statistics().temperatureMin()).isNull();
        assertThat(result.statistics().temperatureAvg()).isNull();
        assertThat(result.statistics().precipitationTotal()).isEqualByComparingTo("0.20");
    }

    @Test
    void mismatchedSeriesTimesRemainIndependentWithoutInventedPoints() {
        rows(point("PRECIP", END, "0.20"), point("T2M", END, "21.10"),
                point("T2M", START, "19.00"), point("PRECIP", START.plusHours(3), "0.40"));
        var result = service.trend(8L, 41L, START, END);
        assertThat(result.temperature()).extracting(p -> p.forecastTime()).containsExactly(
                "2026-09-07 08:00:00", "2026-09-07 14:00:00");
        assertThat(result.precipitation()).extracting(p -> p.forecastTime()).containsExactly(
                "2026-09-07 11:00:00", "2026-09-07 14:00:00");
        assertThat(result.temperature()).extracting(p -> p.value()).containsExactly(
                new BigDecimal("19.00"), new BigDecimal("21.10"));
        assertThat(result.precipitation()).extracting(p -> p.value()).containsExactly(
                new BigDecimal("0.40"), new BigDecimal("0.20"));
        assertThat(result.statistics().temperatureMax()).isEqualByComparingTo("21.10");
        assertThat(result.statistics().temperatureMin()).isEqualByComparingTo("19.00");
        assertThat(result.statistics().temperatureAvg()).isEqualTo(new BigDecimal("20.05"));
        assertThat(result.statistics().precipitationTotal()).isEqualByComparingTo("0.60");
    }

    @Test
    void observedZeroPrecipitationIsNotNull() {
        rows(point("PRECIP", START, "0.00"), point("PRECIP", END, "0.00"));
        var result = service.trend(8L, 41L, START, END);
        assertThat(result.precipitation()).hasSize(2);
        assertThat(result.statistics().precipitationTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void invalidRangeFailsBeforeDatabaseAccess() {
        assertThat(error(8L, 41L, END, START)).isEqualTo(400);
        assertThat(error(8L, 41L, null, END)).isEqualTo(400);
        assertThat(error(8L, 41L, START, null)).isEqualTo(400);
        verifyNoInteractions(records, cities, models, elements);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 9007199254740992L})
    void invalidIdsFailBeforeDatabaseAccess(long invalid) {
        assertThat(error(invalid, 41L, START, END)).isEqualTo(400);
        assertThat(error(8L, invalid, START, END)).isEqualTo(400);
        verifyNoInteractions(records, cities, models, elements);
    }

    @Test
    void nullIdsAre400AndMaximumSafeIdReachesExistenceCheck() {
        assertThat(error(null, 41L, START, END)).isEqualTo(400);
        assertThat(error(8L, null, START, END)).isEqualTo(400);
        assertThat(error(9007199254740991L, 41L, START, END)).isEqualTo(404);
    }

    @Test
    void absentCityOrModelReturns404() {
        assertThat(error(999L, 41L, START, END)).isEqualTo(404);
        assertThat(error(8L, 999L, START, END)).isEqualTo(404);
        verifyNoInteractions(records, elements);
    }

    @Test
    void missingAssociationPrecedesUnsupportedScope() {
        city.setCityCode("OTHER");
        assertThat(error(8L, 999L, START, END)).isEqualTo(404);
    }

    @Test
    void unsupportedCityAndModelReturn400() {
        city.setCityCode("OTHER"); assertThat(error(8L, 41L, START, END)).isEqualTo(400);
        city.setCityCode("JINAN"); model.setModelCode("OTHER");
        assertThat(error(8L, 41L, START, END)).isEqualTo(400);
        verifyNoInteractions(records, elements);
    }

    @Test
    void eitherMissingCoreElementIs500NotEmpty() {
        when(elements.selectList(any())).thenReturn(List.of(precipitation));
        assertThat(error(8L, 41L, START, END)).isEqualTo(500);
        when(elements.selectList(any())).thenReturn(List.of(temperature));
        assertThat(error(8L, 41L, START, END)).isEqualTo(500);
        verifyNoInteractions(records);
    }

    @Test
    void incorrectCoreUnitIsConfigurationError() {
        temperature.setUnit("K"); assertThat(error(8L, 41L, START, END)).isEqualTo(500);
        temperature.setUnit("℃"); precipitation.setUnit("cm");
        assertThat(error(8L, 41L, START, END)).isEqualTo(500);
        verifyNoInteractions(records);
    }

    @Test
    void noaaAndQingdaoAreSupportedAndEqualTimeIsAllowed() {
        city.setCityCode("QINGDAO"); model.setModelCode("NOAA");
        service.trend(8L, 41L, START, START);
        verify(records).selectTrendData(8L, 41L, List.of(71L, 83L), START, START);
    }
}
