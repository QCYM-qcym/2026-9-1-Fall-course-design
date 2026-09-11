package com.shandong.weather;

import com.shandong.weather.mapper.ComparisonRecordRow;
import com.shandong.weather.mapper.ForecastRecordMapper;
import com.shandong.weather.service.WeatherQueryService;
import com.shandong.weather.vo.ComparisonVO;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static com.shandong.weather.WorkbenchTestData.*;
import static org.assertj.core.api.Assertions.assertThat;

// Real MySQL, SELECT only; missing-data cases are isolated in unit tests, not seed mutations.
@SpringBootTest(classes = WeatherApplication.class)
@Transactional(readOnly = true)
class ComparisonMapperIntegrationTests {
    @Autowired private ForecastRecordMapper mapper;
    @Autowired private WeatherQueryService service;
    @Autowired private JdbcTemplate jdbc;
    private long jinan;
    private long temperature;
    private long precipitation;
    private List<Long> modelIds;

    @BeforeEach
    void resolveActualIdsAndVerifyCourseDatabase() {
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("shandong_weather");
        jinan = jdbc.queryForObject("SELECT id FROM city WHERE city_code = ?", Long.class, "JINAN");
        temperature = jdbc.queryForObject("SELECT id FROM weather_element WHERE element_code = ?", Long.class, "T2M");
        precipitation = jdbc.queryForObject("SELECT id FROM weather_element WHERE element_code = ?", Long.class, "PRECIP");
        modelIds = List.of(
                jdbc.queryForObject("SELECT id FROM forecast_model WHERE model_code = ?", Long.class, "ECMWF"),
                jdbc.queryForObject("SELECT id FROM forecast_model WHERE model_code = ?", Long.class, "NOAA"));
        countsUnchanged();
    }

    @AfterEach
    void countsUnchanged() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM city", Long.class)).isEqualTo(16L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM forecast_model", Long.class)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM weather_element", Long.class)).isEqualTo(6L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM forecast_record", Long.class)).isEqualTo(46080L);
    }

    private void metadata(ComparisonVO result, long elementId, String code, String unit) {
        assertThat(result.cityId()).isEqualTo(jinan);
        assertThat(result.cityName()).isEqualTo("济南");
        assertThat(result.element().id()).isEqualTo(elementId);
        assertThat(result.element().elementCode()).isEqualTo(code);
        assertThat(result.element().elementName()).isEqualTo("T2M".equals(code) ? "2 米气温" : "降水量");
        assertThat(result.element().unit()).isEqualTo(unit);
        assertThat(result.series()).extracting(ComparisonVO.ModelSeries::modelId).containsExactlyElementsOf(modelIds);
        assertThat(result.series()).extracting(ComparisonVO.ModelSeries::modelCode).containsExactly("ECMWF", "NOAA");
        assertThat(result.series()).extracting(ComparisonVO.ModelSeries::modelName).containsExactly("ECMWF", "NOAA");
    }

    private void values(List<ComparisonVO.SeriesPoint> points, String... expected) {
        assertThat(points).hasSize(expected.length)
                .isSortedAccordingTo(Comparator.comparing(ComparisonVO.SeriesPoint::forecastTime));
        for (int index = 0; index < expected.length; index++) {
            assertThat(points.get(index).value()).isEqualByComparingTo(new BigDecimal(expected[index]));
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"T2M,℃", "PRECIP,mm", "TCC,%", "WIND_SPEED_100M,m/s", "WIND_DIR_100M,°", "RH,%"})
    void monthlyElementsReturn240PointsPerModel(String code, String unit) {
        long element = jdbc.queryForObject("SELECT id FROM weather_element WHERE element_code = ?", Long.class, code);
        var start = java.time.LocalDateTime.of(2026, 9, 1, 2, 0);
        var end = java.time.LocalDateTime.of(2026, 9, 30, 23, 0);
        var expectedTimes = java.util.stream.IntStream.range(0, 240).mapToObj(i -> start.plusHours(i * 3L)).toList();
        var rows = mapper.selectComparisonData(jinan, element, modelIds, start, end);
        assertThat(rows).hasSize(480);
        for (String model : List.of("ECMWF", "NOAA"))
            assertThat(rows.stream().filter(r -> model.equals(r.modelCode())).map(ComparisonRecordRow::forecastTime).sorted().toList())
                    .containsExactlyElementsOf(expectedTimes);
        var response = service.comparison(jinan, element, start, end);
        assertThat(response.element().elementCode()).isEqualTo(code);
        assertThat(response.element().unit()).isEqualTo(unit);
        assertThat(response.series()).extracting(ComparisonVO.ModelSeries::modelCode).containsExactly("ECMWF", "NOAA");
        assertThat(response.series()).allSatisfy(series -> assertThat(series.values()).hasSize(240));
    }

    private ComparisonVO fullRange(long elementId) {
        var rows = mapper.selectComparisonData(jinan, elementId, modelIds, START, END);
        assertThat(rows).hasSize(6).isSortedAccordingTo(Comparator.comparing(ComparisonRecordRow::modelCode)
                .thenComparing(ComparisonRecordRow::forecastTime));
        assertThat(rows).extracting(ComparisonRecordRow::modelCode).containsOnly("ECMWF", "NOAA");
        var result = service.comparison(jinan, elementId, START, END);
        for (var series : result.series()) {
            assertThat(series.values()).extracting(ComparisonVO.SeriesPoint::forecastTime).containsExactly(
                    "2026-09-07 08:00:00", "2026-09-07 11:00:00", "2026-09-07 14:00:00");
        }
        return result;
    }

    @Test
    void jinanTemperatureReturnsThreeRealPointsForEachModel() {
        var result = fullRange(temperature);
        metadata(result, temperature, "T2M", "℃");
        values(result.series().get(0).values(), "19.00", "20.20", "21.10");
        values(result.series().get(1).values(), "18.40", "19.60", "20.50");
    }

    @Test
    void jinanPrecipitationReturnsBothModelsAndKeepsTrueZero() {
        var result = fullRange(precipitation);
        metadata(result, precipitation, "PRECIP", "mm");
        values(result.series().get(0).values(), "0.00", "0.40", "0.20");
        values(result.series().get(1).values(), "0.10", "0.60", "0.30");
    }

    @Test
    void equalEndpointsIncludeOnePointPerModelForBothElements() {
        for (long element : List.of(temperature, precipitation)) {
            assertThat(mapper.selectComparisonData(jinan, element, modelIds, START, START)).hasSize(2);
            var result = service.comparison(jinan, element, START, START);
            assertThat(result.series()).hasSize(2);
            values(result.series().get(0).values(), element == temperature ? "19.00" : "0.00");
            values(result.series().get(1).values(), element == temperature ? "18.40" : "0.10");
            assertThat(result.series()).allSatisfy(s -> assertThat(s.values().get(0).forecastTime())
                    .isEqualTo("2026-09-07 08:00:00"));
        }
    }

    @Test
    void emptyDateRetainsBothModelsAndDictionaryMetadata() {
        assertThat(mapper.selectComparisonData(jinan, temperature, modelIds, START.plusMonths(1), END.plusMonths(1))).isEmpty();
        var result = service.comparison(jinan, temperature, START.plusMonths(1), END.plusMonths(1));
        metadata(result, temperature, "T2M", "℃");
        assertThat(result.series()).allSatisfy(s -> assertThat(s.values()).isEmpty());
    }
}
