package com.shandong.weather;

import com.shandong.weather.entity.City;
import com.shandong.weather.entity.ForecastModel;
import com.shandong.weather.entity.WeatherElement;
import com.shandong.weather.service.CityService;
import com.shandong.weather.service.ForecastModelService;
import com.shandong.weather.service.WeatherElementService;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

// Real Service -> BaseMapper -> MySQL, SELECT only; no mocks or seed writes.
@SpringBootTest(classes = WeatherApplication.class)
@Transactional(readOnly = true)
class DictionaryMapperIntegrationTests {
    @Autowired
    private CityService cities;
    @Autowired
    private ForecastModelService models;
    @Autowired
    private WeatherElementService elements;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void checksActualCourseDatabase() {
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("shandong_weather");
        seedCountsRemainUnchanged();
    }

    @AfterEach
    void seedCountsRemainUnchanged() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM city", Long.class)).isEqualTo(16L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM forecast_model", Long.class)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM weather_element", Long.class)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM forecast_record", Long.class)).isEqualTo(192L);
    }

    @Test
    void citiesMapAllFieldsAndReturnSixteenUniqueCodesInIdOrder() {
        var result = cities.listAll();
        assertThat(result).hasSize(16).isSortedAccordingTo(Comparator.comparing(City::getId));
        assertThat(result).extracting(City::getId).doesNotHaveDuplicates().doesNotContainNull();
        assertThat(result).extracting(City::getCityCode).doesNotHaveDuplicates().doesNotContainNull()
                .contains("JINAN", "QINGDAO");
        assertThat(result).extracting(City::getCityName).contains("济南", "青岛");
        assertThat(result).allSatisfy(city -> {
            assertThat(city.getId()).isBetween(1L, 9007199254740991L);
            assertThat(city.getLongitude()).isNotNull();
            assertThat(city.getLatitude()).isNotNull();
        });
        var jinan = result.stream().filter(c -> "JINAN".equals(c.getCityCode())).findFirst().orElseThrow();
        assertThat(jinan.getCityName()).isEqualTo("济南");
        assertThat(jinan.getLongitude()).isEqualByComparingTo("116.997222");
        assertThat(jinan.getLatitude()).isEqualByComparingTo("36.668333");
        var qingdao = result.stream().filter(c -> "QINGDAO".equals(c.getCityCode())).findFirst().orElseThrow();
        assertThat(qingdao.getCityName()).isEqualTo("青岛");
        assertThat(qingdao.getLongitude()).isEqualByComparingTo("120.380420");
        assertThat(qingdao.getLatitude()).isEqualByComparingTo("36.064880");
    }

    @Test
    void modelsMapNamesAndDescriptionsInIdOrder() {
        var result = models.listAll();
        assertThat(result).hasSize(2).isSortedAccordingTo(Comparator.comparing(ForecastModel::getId));
        assertThat(result).extracting(ForecastModel::getId).doesNotHaveDuplicates().doesNotContainNull();
        assertThat(result).extracting(ForecastModel::getModelCode).doesNotHaveDuplicates()
                .containsExactly("ECMWF", "NOAA");
        assertThat(result).extracting(ForecastModel::getModelName).containsExactly("ECMWF", "NOAA");
        assertThat(result).extracting(ForecastModel::getDescription)
                .containsExactly("欧洲中期天气预报中心模型", "美国国家海洋和大气管理局模型");
    }

    @Test
    void elementsMapUniqueCodesNamesAndCorrectUnitsInIdOrder() {
        var result = elements.listAll();
        assertThat(result).hasSize(2).isSortedAccordingTo(Comparator.comparing(WeatherElement::getId));
        assertThat(result).extracting(WeatherElement::getId).doesNotHaveDuplicates().doesNotContainNull();
        assertThat(result).extracting(WeatherElement::getElementCode).doesNotHaveDuplicates()
                .containsExactly("T2M", "PRECIP");
        assertThat(result).extracting(WeatherElement::getElementName).containsExactly("2 米气温", "降水量");
        assertThat(result).extracting(WeatherElement::getUnit).containsExactly("℃", "mm");
    }
}
