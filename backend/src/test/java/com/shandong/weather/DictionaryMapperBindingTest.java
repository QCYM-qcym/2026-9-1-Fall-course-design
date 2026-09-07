package com.shandong.weather;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shandong.weather.entity.City;
import com.shandong.weather.entity.ForecastModel;
import com.shandong.weather.entity.WeatherElement;
import com.shandong.weather.mapper.CityMapper;
import com.shandong.weather.mapper.ForecastModelMapper;
import com.shandong.weather.mapper.WeatherElementMapper;
import java.util.Map;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Verifies real Mapper registration and generated SQL without opening a DB connection.
@SpringBootTest(classes = WeatherApplication.class)
class DictionaryMapperBindingTest {
    @Autowired
    private SqlSessionFactory factory;
    @Autowired
    private CityMapper cities;
    @Autowired
    private ForecastModelMapper models;
    @Autowired
    private WeatherElementMapper elements;

    @Test
    void cityMapperSelectsFrozenColumnsFromCityWithExplicitOrder() {
        assertQuery(CityMapper.class, new QueryWrapper<City>().orderByAsc("id"),
                "SELECT id,city_code,city_name,longitude,latitude FROM city ORDER BY id ASC");
    }

    @Test
    void modelMapperSelectsDescriptionAndCamelCaseColumnsWithExplicitOrder() {
        assertQuery(ForecastModelMapper.class, new QueryWrapper<ForecastModel>().orderByAsc("id"),
                "SELECT id,model_code,model_name,description FROM forecast_model ORDER BY id ASC");
    }

    @Test
    void elementMapperSelectsUnitAndCamelCaseColumnsWithExplicitOrder() {
        assertQuery(WeatherElementMapper.class, new QueryWrapper<WeatherElement>().orderByAsc("id"),
                "SELECT id,element_code,element_name,unit FROM weather_element ORDER BY id ASC");
    }

    private void assertQuery(Class<?> mapper, QueryWrapper<?> query, String expected) {
        var configuration = factory.getConfiguration();
        assertThat(configuration.hasMapper(mapper)).isTrue();
        assertThat(configuration.isMapUnderscoreToCamelCase()).isTrue();
        var sql = configuration.getMappedStatement(mapper.getName() + ".selectList")
                .getBoundSql(Map.of("ew", query));
        assertThat(sql.getSql().replaceAll("\\s+", " ").trim()).isEqualTo(expected);
        assertThat(sql.getParameterMappings()).isEmpty();
    }
}
