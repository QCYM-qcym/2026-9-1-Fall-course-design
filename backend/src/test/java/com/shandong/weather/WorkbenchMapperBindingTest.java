package com.shandong.weather;

import com.shandong.weather.mapper.ForecastRecordMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.shandong.weather.WorkbenchTestData.*;
import static org.assertj.core.api.Assertions.assertThat;

// Loads the real Mapper/XML without opening a database connection.
@SpringBootTest(classes = WeatherApplication.class)
class WorkbenchMapperBindingTest {
    @Autowired
    private SqlSessionFactory factory;
    @Autowired
    private ForecastRecordMapper mapper;

    @Test
    void registeredMapperBuildsParameterizedFourTableQuery() {
        var configuration = factory.getConfiguration();
        assertThat(configuration.hasMapper(ForecastRecordMapper.class)).isTrue();
        var statement = configuration.getMappedStatement(
                ForecastRecordMapper.class.getName() + ".selectWorkbenchData");
        var sql = statement.getBoundSql(Map.of("modelId", 41L, "elementId", 71L,
                "startTime", START, "endTime", END, "cityCodes", CITIES));
        var normalized = sql.getSql().replaceAll("\\s+", " ").trim();
        assertThat(normalized).contains("JOIN city c ON fr.city_id = c.id",
                "JOIN forecast_model fm ON fr.model_id = fm.id",
                "JOIN weather_element we ON fr.element_id = we.id",
                "fr.model_id = ?", "fr.element_id = ?", "fr.forecast_time BETWEEN ? AND ?",
                "c.city_code IN", "ORDER BY fr.forecast_time ASC, fr.city_id ASC");
        assertThat(normalized).doesNotContain("JINAN", "QINGDAO", "2026-09-07");
        assertThat(sql.getParameterMappings()).hasSize(6);
        assertThat(sql.getParameterMappings().subList(0, 4)).extracting(p -> p.getProperty())
                .containsExactly("modelId", "elementId", "startTime", "endTime");
    }

    @Test
    void emptyCityScopeProducesFalsePredicateInsteadOfUnrestrictedQuery() {
        var statement = factory.getConfiguration().getMappedStatement(
                ForecastRecordMapper.class.getName() + ".selectWorkbenchData");
        var sql = statement.getBoundSql(Map.of("modelId", 41L, "elementId", 71L,
                "startTime", START, "endTime", END, "cityCodes", List.of()));
        assertThat(sql.getSql().replaceAll("\\s+", " ")).contains("AND 1 = 0");
    }
}
