package com.shandong.weather;

import com.shandong.weather.mapper.ForecastRecordMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import java.util.Map;
import static com.shandong.weather.WorkbenchTestData.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WeatherApplication.class)
class TrendMapperBindingTest {
    @Autowired private SqlSessionFactory factory;

    @Test
    void trendStatementIsRegisteredInExistingMapperNamespace() {
        assertThat(factory.getConfiguration().hasStatement(
                ForecastRecordMapper.class.getName() + ".selectTrendData")).isTrue();
    }

    @Test
    void trendQueryBindsAllFiltersAndMapsTypedProjection() {
        var statement = factory.getConfiguration().getMappedStatement(
                ForecastRecordMapper.class.getName() + ".selectTrendData");
        var bound = statement.getBoundSql(Map.of("cityId", 8L, "modelId", 41L,
                "elementIds", List.of(71L, 83L), "startTime", START, "endTime", END));
        assertThat(bound.getSql().replaceAll("\\s+", " ")).contains(
                "we.element_code", "fr.forecast_time", "fr.value", "JOIN weather_element we",
                "fr.city_id = ?", "fr.model_id = ?", "fr.element_id IN",
                "fr.forecast_time BETWEEN ? AND ?", "ORDER BY we.element_code ASC, fr.forecast_time ASC")
                .doesNotContain("2026-09-07", "${");
        var parameters = bound.getParameterMappings();
        assertThat(parameters).hasSize(6);
        assertThat(parameters.get(0).getProperty()).isEqualTo("cityId");
        assertThat(parameters.get(1).getProperty()).isEqualTo("modelId");
        assertThat(bound.getAdditionalParameter(parameters.get(2).getProperty())).isEqualTo(71L);
        assertThat(bound.getAdditionalParameter(parameters.get(3).getProperty())).isEqualTo(83L);
        assertThat(parameters.get(4).getProperty()).isEqualTo("startTime");
        assertThat(parameters.get(5).getProperty()).isEqualTo("endTime");
        var result = statement.getResultMaps().get(0);
        assertThat(result.getType()).isEqualTo(com.shandong.weather.mapper.TrendRecordRow.class);
        assertThat(result.getConstructorResultMappings()).extracting(r -> r.getColumn())
                .containsExactly("element_code", "forecast_time", "value");
    }

    @Test
    void emptyElementScopeNeverFallsBackToUnrestrictedQuery() {
        var statement = factory.getConfiguration().getMappedStatement(
                ForecastRecordMapper.class.getName() + ".selectTrendData");
        var bound = statement.getBoundSql(Map.of("cityId", 8L, "modelId", 41L,
                "elementIds", List.of(), "startTime", START, "endTime", END));
        assertThat(bound.getSql().replaceAll("\\s+", " ")).contains("AND 1 = 0");
    }
}
