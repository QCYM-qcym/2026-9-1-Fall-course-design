package com.shandong.weather;

import com.shandong.weather.mapper.ForecastRecordMapper;
import com.shandong.weather.mapper.WorkbenchRecordRow;
import com.shandong.weather.service.WeatherQueryService;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static com.shandong.weather.WorkbenchTestData.START;
import static com.shandong.weather.WorkbenchTestData.END;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WeatherApplication.class)
@Transactional(readOnly = true)
class WorkbenchMapperIntegrationTests {
    @Autowired
    private ForecastRecordMapper mapper;
    @Autowired
    private WeatherQueryService service;
    @Autowired
    private JdbcTemplate jdbc;
    @Value("${weather.workbench.city-codes}")
    private List<String> cityCodes;

    private long ecmwf;
    private long noaa;
    private long temperature;
    private long precipitation;
    private long jinan;

    @BeforeEach
    void resolveActualDictionaryIds() {
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("shandong_weather");
        ecmwf = jdbc.queryForObject("SELECT id FROM forecast_model WHERE model_code = ?", Long.class, "ECMWF");
        noaa = jdbc.queryForObject("SELECT id FROM forecast_model WHERE model_code = ?", Long.class, "NOAA");
        temperature = jdbc.queryForObject("SELECT id FROM weather_element WHERE element_code = ?", Long.class, "T2M");
        precipitation = jdbc.queryForObject("SELECT id FROM weather_element WHERE element_code = ?", Long.class, "PRECIP");
        jinan = jdbc.queryForObject("SELECT id FROM city WHERE city_code = ?", Long.class, "JINAN");
    }

    @AfterEach
    void seedCountsRemainUnchanged() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM city", Long.class)).isEqualTo(16L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM forecast_model", Long.class)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM weather_element", Long.class)).isEqualTo(6L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM forecast_record", Long.class)).isEqualTo(46080L);
    }

    private void assertCompleteBatch(List<WorkbenchRecordRow> rows) {
        assertThat(rows).hasSize(48).isSortedAccordingTo(
                Comparator.comparing(WorkbenchRecordRow::forecastTime).thenComparing(WorkbenchRecordRow::cityId));
        assertThat(rows.stream().map(WorkbenchRecordRow::forecastTime).distinct().toList())
                .containsExactly(START, START.plusHours(3), END);
        for (var time : List.of(START, START.plusHours(3), END)) {
            assertThat(rows.stream().filter(r -> r.forecastTime().equals(time))
                    .map(WorkbenchRecordRow::cityId).distinct().toList()).hasSize(16);
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"T2M,℃", "PRECIP,mm", "TCC,%", "WIND_SPEED_100M,m/s", "WIND_DIR_100M,°", "RH,%"})
    void monthlyElementsReturnEveryCityAndTimestampForBothModels(String code, String unit) {
        long element = jdbc.queryForObject("SELECT id FROM weather_element WHERE element_code = ?", Long.class, code);
        var start = java.time.LocalDateTime.of(2026, 9, 1, 2, 0);
        var end = java.time.LocalDateTime.of(2026, 9, 30, 23, 0);
        var expectedTimes = java.util.stream.IntStream.range(0, 240).mapToObj(i -> start.plusHours(i * 3L)).toList();
        for (long model : List.of(ecmwf, noaa)) {
            var rows = mapper.selectWorkbenchData(model, element, start, end, cityCodes);
            assertThat(rows).hasSize(3840);
            assertThat(rows.stream().map(WorkbenchRecordRow::forecastTime).distinct().sorted().toList()).containsExactlyElementsOf(expectedTimes);
            var byCity = rows.stream().collect(java.util.stream.Collectors.groupingBy(WorkbenchRecordRow::cityId));
            assertThat(byCity).hasSize(16);
            byCity.values().forEach(points -> assertThat(points.stream().map(WorkbenchRecordRow::forecastTime).sorted().toList())
                    .containsExactlyElementsOf(expectedTimes));
            var response = service.workbench(model, element, start, end);
            assertThat(response.element().elementCode()).isEqualTo(code);
            assertThat(response.element().unit()).isEqualTo(unit);
            assertThat(response.times()).hasSize(240);
            assertThat(response.records()).hasSize(3840);
        }
    }

    @Test
    void ecmwfTemperatureJoinsAllCitiesAndMatchesSyntheticJinanValues() {
        var rows = mapper.selectWorkbenchData(ecmwf, temperature, START, END, cityCodes);
        assertCompleteBatch(rows);
        var values = rows.stream().filter(r -> r.cityId() == jinan).map(WorkbenchRecordRow::value).toList();
        assertThat(values).hasSize(3);
        assertThat(values.get(0)).isEqualByComparingTo("19.00");
        assertThat(values.get(1)).isEqualByComparingTo("20.20");
        assertThat(values.get(2)).isEqualByComparingTo("21.10");
        assertThat(rows.stream().filter(r -> r.cityId() == jinan).map(WorkbenchRecordRow::cityName).distinct())
                .containsExactly("济南");
        var response = service.workbench(ecmwf, temperature, START, END);
        assertThat(response.model().modelCode()).isEqualTo("ECMWF");
        assertThat(response.element().elementCode()).isEqualTo("T2M");
        assertThat(response.element().unit()).isEqualTo("℃");
        assertThat(response.records()).hasSize(48);
        assertThat(response.times()).containsExactly(
                "2026-09-07 08:00:00", "2026-09-07 11:00:00", "2026-09-07 14:00:00");
    }

    @Test
    void noaaUsesSameMapperAndServiceWithoutModelSpecificQuery() {
        assertCompleteBatch(mapper.selectWorkbenchData(noaa, temperature, START, END, cityCodes));
        var response = service.workbench(noaa, temperature, START, END);
        assertThat(response.model().modelCode()).isEqualTo("NOAA");
        assertThat(response.records()).hasSize(48);
        assertThat(response.records().stream().filter(r -> r.cityId() == jinan).findFirst().orElseThrow().value())
                .isEqualByComparingTo("18.40");
    }

    @Test
    void precipitationIsQueryableAndNonnegativeForBothModels() {
        for (long modelId : List.of(ecmwf, noaa)) {
            var rows = mapper.selectWorkbenchData(modelId, precipitation, START, END, cityCodes);
            assertCompleteBatch(rows);
            assertThat(rows).allSatisfy(row -> assertThat(row.value()).isNotNegative());
            var response = service.workbench(modelId, precipitation, START, END);
            assertThat(response.element().elementCode()).isEqualTo("PRECIP");
            assertThat(response.element().unit()).isEqualTo("mm");
            assertThat(response.records()).hasSize(48);
        }
    }

    @Test
    void closedIntervalAndEmptyRangeFollowContract() {
        assertThat(mapper.selectWorkbenchData(ecmwf, temperature, START, START, cityCodes)).hasSize(16);
        var empty = service.workbench(ecmwf, temperature, START.plusYears(1), END.plusYears(1));
        assertThat(empty.times()).isEmpty();
        assertThat(empty.records()).isEmpty();
        assertThat(empty.model().modelCode()).isEqualTo("ECMWF");
        assertThat(empty.element().unit()).isEqualTo("℃");
    }

    @Test
    void cityScopeIsBoundAndNeverFallsBackToAllCities() {
        assertThat(mapper.selectWorkbenchData(ecmwf, temperature, START, END, List.of("JINAN")))
                .hasSize(3).allSatisfy(row -> assertThat(row.cityId()).isEqualTo(jinan));
        assertThat(mapper.selectWorkbenchData(ecmwf, temperature, START, END, List.of())).isEmpty();
        assertThat(mapper.selectWorkbenchData(ecmwf, temperature, START, END, List.of("JINAN') OR 1=1 --")))
                .isEmpty();
    }
}
