package com.shandong.weather;

import com.shandong.weather.controller.CityController;
import com.shandong.weather.controller.ForecastModelController;
import com.shandong.weather.controller.WeatherElementController;
import com.shandong.weather.mapper.CityMapper;
import com.shandong.weather.mapper.ForecastModelMapper;
import com.shandong.weather.mapper.WeatherElementMapper;
import com.shandong.weather.service.CityService;
import com.shandong.weather.service.ForecastModelService;
import com.shandong.weather.service.WeatherElementService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({CityController.class, ForecastModelController.class, WeatherElementController.class})
@Import({CityService.class, ForecastModelService.class, WeatherElementService.class})
class DictionaryControllerTest {
    @Autowired
    private MockMvc mvc;
    // Controllers and Services stay real; replace only the database boundary.
    @MockBean
    private CityMapper cities;
    @MockBean
    private ForecastModelMapper models;
    @MockBean
    private WeatherElementMapper elements;
    @MockBean
    private com.shandong.weather.mapper.ForecastRecordMapper records;

    @BeforeEach
    void emptyTables() {
        when(cities.selectList(any())).thenReturn(List.of());
        when(models.selectList(any())).thenReturn(List.of());
        when(elements.selectList(any())).thenReturn(List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/cities", "/api/forecast-models", "/api/weather-elements"})
    void dictionaryRouteReturnsSuccessArrayForEmptyTable(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", empty()));
    }

    @Test
    void citiesExposeExactlyFrozenFieldsAndNumericCoordinates() throws Exception {
        when(cities.selectList(any())).thenReturn(DictionaryTestData.cities());
        mvc.perform(get("/api/cities")).andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":200,"message":"success","data":[
                          {"id":8,"cityCode":"JINAN","cityName":"济南",
                           "longitude":116.997222,"latitude":36.668333},
                          {"id":40,"cityCode":"TEMP_CITY","cityName":"临时城市",
                           "longitude":120.380420,"latitude":36.064880}
                        ]}
                        """, true));
    }

    @Test
    void modelsKeepDatabaseNamesAndNullableDescriptionField() throws Exception {
        when(models.selectList(any())).thenReturn(DictionaryTestData.models());
        mvc.perform(get("/api/forecast-models")).andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":200,"message":"success","data":[
                          {"id":41,"modelCode":"ECMWF","modelName":"数据库模型名称",
                           "description":"数据库模型说明"},
                          {"id":90,"modelCode":"TEMP_MODEL","modelName":"临时模型","description":null}
                        ]}
                        """, true));
    }

    @Test
    void elementsKeepDatabaseNamesAndUnicodeUnitsWithoutFiltering() throws Exception {
        when(elements.selectList(any())).thenReturn(DictionaryTestData.elements());
        mvc.perform(get("/api/weather-elements")).andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":200,"message":"success","data":[
                          {"id":71,"elementCode":"T2M","elementName":"2 米气温","unit":"℃"},
                          {"id":80,"elementCode":"TEMP_ELEMENT","elementName":"临时要素","unit":"mm"}
                        ]}
                        """, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/cities", "/api/forecast-models", "/api/weather-elements"})
    void databaseFailureUsesExistingSafe500Envelope(String path) throws Exception {
        var failure = new IllegalStateException("private SQL details");
        when(cities.selectList(any())).thenThrow(failure);
        when(models.selectList(any())).thenThrow(failure);
        when(elements.selectList(any())).thenThrow(failure);
        mvc.perform(get(path)).andExpect(status().isInternalServerError())
                .andExpect(content().json(
                        "{\"code\":500,\"message\":\"internal server error\",\"data\":null}", true));
    }
}
