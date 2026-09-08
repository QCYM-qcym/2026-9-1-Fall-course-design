package com.shandong.weather;

import com.shandong.weather.mapper.ForecastRecordMapper;
import com.shandong.weather.mapper.ComparisonRecordRow;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static com.shandong.weather.WorkbenchTestData.*;

@SpringBootTest(classes = WeatherApplication.class)
class ComparisonMapperBindingTest {
    @Autowired private SqlSessionFactory factory;

    @Test
    void comparisonStatementIsRegisteredInExistingNamespace() {
        assertThat(factory.getConfiguration().hasStatement(
                ForecastRecordMapper.class.getName() + ".selectComparisonData")).isTrue();
    }

    @Test
    void bindsAllFiltersAndMapsTypedProjection() {
        var config = factory.getConfiguration();
        var id = ForecastRecordMapper.class.getName() + ".selectComparisonData";
        assertThat(config.hasStatement(id)).isTrue();
        var statement = config.getMappedStatement(id);
        var bound = statement.getBoundSql(Map.of("cityId", 8L, "elementId", 71L,
                "modelIds", List.of(41L, 59L), "startTime", START, "endTime", END));
        assertThat(bound.getSql().replaceAll("\\s+", " ")).contains(
                "fm.model_code", "fr.forecast_time", "fr.value", "JOIN forecast_model fm ON fr.model_id = fm.id",
                "fr.city_id = ?", "fr.element_id = ?", "fr.model_id IN",
                "fr.forecast_time BETWEEN ? AND ?", "ORDER BY fm.model_code ASC, fr.forecast_time ASC")
                .doesNotContain("${", "2026-09-07");
        var params = bound.getParameterMappings();
        assertThat(params).hasSize(6);
        assertThat(params.get(0).getProperty()).isEqualTo("cityId");
        assertThat(params.get(1).getProperty()).isEqualTo("elementId");
        assertThat(bound.getAdditionalParameter(params.get(2).getProperty())).isEqualTo(41L);
        assertThat(bound.getAdditionalParameter(params.get(3).getProperty())).isEqualTo(59L);
        assertThat(params.get(4).getProperty()).isEqualTo("startTime");
        assertThat(params.get(5).getProperty()).isEqualTo("endTime");
        var result = statement.getResultMaps().get(0);
        assertThat(result.getType()).isEqualTo(ComparisonRecordRow.class);
        assertThat(result.getConstructorResultMappings()).extracting(r -> r.getColumn())
                .containsExactly("model_code", "forecast_time", "value");
    }

    @Test
    void nullOrEmptyModelScopeCannotBecomeUnrestrictedQuery() {
        var config = factory.getConfiguration();
        var id = ForecastRecordMapper.class.getName() + ".selectComparisonData";
        assertThat(config.hasStatement(id)).isTrue();
        var params = new HashMap<String, Object>(Map.of("cityId", 8L, "elementId", 71L,
                "startTime", START, "endTime", END));
        for (int index = 0; index < 2; index++) {
            params.put("modelIds", index == 0 ? null : List.of());
            var sql = config.getMappedStatement(id).getBoundSql(params).getSql().replaceAll("\\s+", " ");
            assertThat(sql).contains("AND 1 = 0").doesNotContain("IN ()");
        }
    }
}
