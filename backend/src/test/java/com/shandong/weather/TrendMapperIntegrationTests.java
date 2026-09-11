package com.shandong.weather;

import com.shandong.weather.mapper.ForecastRecordMapper;
import com.shandong.weather.mapper.TrendRecordRow;
import com.shandong.weather.service.WeatherQueryService;
import com.shandong.weather.vo.TrendVO;
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

// Real MySQL, SELECT only. This class never creates, updates or deletes test data.
@SpringBootTest(classes = WeatherApplication.class)
@Transactional(readOnly = true)
class TrendMapperIntegrationTests {
    @Autowired private ForecastRecordMapper mapper;
    @Autowired private WeatherQueryService service;
    @Autowired private JdbcTemplate jdbc;
    private long jinan;
    private long ecmwf;
    private long noaa;
    private List<Long> elementIds;

    @BeforeEach
    void resolveRealIdsByCodeAndCheckDatabase() {
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("shandong_weather");
        jinan = jdbc.queryForObject("SELECT id FROM city WHERE city_code = ?", Long.class, "JINAN");
        ecmwf = jdbc.queryForObject("SELECT id FROM forecast_model WHERE model_code = ?", Long.class, "ECMWF");
        noaa = jdbc.queryForObject("SELECT id FROM forecast_model WHERE model_code = ?", Long.class, "NOAA");
        elementIds = List.of(
                jdbc.queryForObject("SELECT id FROM weather_element WHERE element_code = ?", Long.class, "T2M"),
                jdbc.queryForObject("SELECT id FROM weather_element WHERE element_code = ?", Long.class, "PRECIP"));
        countsUnchanged();
    }

    @AfterEach
    void countsUnchanged() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM city", Long.class)).isEqualTo(16L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM forecast_model", Long.class)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM weather_element", Long.class)).isEqualTo(6L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM forecast_record", Long.class)).isEqualTo(46080L);
    }

    private void values(List<TrendVO.SeriesPoint> points, String... expected) {
        assertThat(points).hasSize(expected.length).isSortedAccordingTo(Comparator.comparing(TrendVO.SeriesPoint::forecastTime));
        for (int index = 0; index < expected.length; index++) {
            assertThat(points.get(index).value()).isEqualByComparingTo(new BigDecimal(expected[index]));
        }
    }

    @Test
    void jinanEcmwfReturnsBothSeedSeriesAndFourStatistics() {
        var rows = mapper.selectTrendData(jinan, ecmwf, elementIds, START, END);
        assertThat(rows).hasSize(6).isSortedAccordingTo(Comparator.comparing(TrendRecordRow::elementCode)
                .thenComparing(TrendRecordRow::forecastTime));
        assertThat(rows).extracting(TrendRecordRow::elementCode).containsOnly("T2M", "PRECIP");
        var result = service.trend(jinan, ecmwf, START, END);
        assertThat(result.cityId()).isEqualTo(jinan); assertThat(result.cityName()).isEqualTo("济南");
        assertThat(result.modelId()).isEqualTo(ecmwf); assertThat(result.modelName()).isEqualTo("ECMWF");
        values(result.temperature(), "19.00", "20.20", "21.10");
        values(result.precipitation(), "0.00", "0.40", "0.20");
        for (var series : List.of(result.temperature(), result.precipitation())) {
            assertThat(series).extracting(TrendVO.SeriesPoint::forecastTime).containsExactly(
                    "2026-09-07 08:00:00", "2026-09-07 11:00:00", "2026-09-07 14:00:00");
        }
        assertThat(result.statistics().temperatureMax()).isEqualByComparingTo("21.10");
        assertThat(result.statistics().temperatureMin()).isEqualByComparingTo("19.00");
        assertThat(result.statistics().temperatureAvg()).isEqualByComparingTo("20.10");
        assertThat(result.statistics().precipitationTotal()).isEqualByComparingTo("0.60");
    }

    @Test
    void noaaUsesSameQueryWithoutEcmwfLeakage() {
        assertThat(mapper.selectTrendData(jinan, noaa, elementIds, START, END)).hasSize(6);
        var result = service.trend(jinan, noaa, START, END);
        assertThat(result.modelName()).isEqualTo("NOAA");
        values(result.temperature(), "18.40", "19.60", "20.50");
        values(result.precipitation(), "0.10", "0.60", "0.30");
        assertThat(result.statistics().temperatureAvg()).isEqualByComparingTo("19.50");
        assertThat(result.statistics().precipitationTotal()).isEqualByComparingTo("1.00");
    }

    @Test
    void equalEndpointsIncludeBothElementsAndKeepTrueZero() {
        assertThat(mapper.selectTrendData(jinan, ecmwf, elementIds, START, START)).hasSize(2);
        var result = service.trend(jinan, ecmwf, START, START);
        values(result.temperature(), "19.00"); values(result.precipitation(), "0.00");
        assertThat(result.statistics().precipitationTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void emptyRangeHasNoRowsAndPreservesMetadataWithNullStatistics() {
        assertThat(mapper.selectTrendData(jinan, ecmwf, elementIds, START.plusYears(1), END.plusYears(1))).isEmpty();
        var result = service.trend(jinan, ecmwf, START.plusYears(1), END.plusYears(1));
        assertThat(result.cityName()).isEqualTo("济南"); assertThat(result.modelName()).isEqualTo("ECMWF");
        assertThat(result.temperature()).isEmpty(); assertThat(result.precipitation()).isEmpty();
        assertThat(result.statistics().temperatureMax()).isNull();
        assertThat(result.statistics().temperatureMin()).isNull();
        assertThat(result.statistics().temperatureAvg()).isNull();
        assertThat(result.statistics().precipitationTotal()).isNull();
    }
}
