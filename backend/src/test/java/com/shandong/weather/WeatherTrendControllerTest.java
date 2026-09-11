package com.shandong.weather;

import com.shandong.weather.controller.WeatherQueryController;
import com.shandong.weather.mapper.ForecastRecordMapper;
import com.shandong.weather.service.WeatherQueryService;
import com.shandong.weather.mapper.TrendRecordRow;
import com.shandong.weather.entity.WeatherElement;
import java.math.BigDecimal;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.shandong.weather.WorkbenchTestData.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest(WeatherQueryController.class)
@Import(WeatherQueryService.class)
@BusinessSecurityTest
@org.springframework.security.test.context.support.WithMockUser(roles = "USER")
class WeatherTrendControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private ForecastRecordMapper mapper;
    @MockBean private com.shandong.weather.mapper.CityMapper cities;
    @MockBean private com.shandong.weather.mapper.ForecastModelMapper models;
    @MockBean private com.shandong.weather.mapper.WeatherElementMapper elements;

    @BeforeEach
    void dictionariesAndSixSamples() {
        when(cities.selectById(8L)).thenReturn(DictionaryTestData.cities().get(0));
        when(models.selectById(41L)).thenReturn(DictionaryTestData.models().get(0));
        var precip = new WeatherElement();
        precip.setId(83L); precip.setElementCode("PRECIP"); precip.setElementName("降水量"); precip.setUnit("mm");
        when(elements.selectList(any())).thenReturn(List.of(DictionaryTestData.elements().get(0), precip));
        when(mapper.selectTrendData(8L, 41L, List.of(71L, 83L), START, END)).thenReturn(List.of(
                new TrendRecordRow("T2M", START, new BigDecimal("19.00")),
                new TrendRecordRow("T2M", START.plusHours(3), new BigDecimal("20.20")),
                new TrendRecordRow("T2M", END, new BigDecimal("21.10")),
                new TrendRecordRow("PRECIP", START, new BigDecimal("0.00")),
                new TrendRecordRow("PRECIP", START.plusHours(3), new BigDecimal("0.40")),
                new TrendRecordRow("PRECIP", END, new BigDecimal("0.20"))));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request() {
        return get("/api/weather/trend").param("cityId", "8").param("modelId", "41")
                .param("startTime", "2026-09-07 08:00:00").param("endTime", "2026-09-07 14:00:00");
    }

    private void error(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            int code) throws Exception {
        mvc.perform(request).andExpect(status().is(code)).andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void responseHasExactFlatContractAndNumericStatistics() throws Exception {
        mvc.perform(request()).andExpect(status().isOk()).andExpect(content().json("""
                {"code":200,"message":"success","data":{
                  "cityId":8,"cityName":"济南","modelId":41,"modelName":"数据库模型名称",
                  "temperature":[
                    {"forecastTime":"2026-09-07 08:00:00","value":19.00},
                    {"forecastTime":"2026-09-07 11:00:00","value":20.20},
                    {"forecastTime":"2026-09-07 14:00:00","value":21.10}],
                  "precipitation":[
                    {"forecastTime":"2026-09-07 08:00:00","value":0.00},
                    {"forecastTime":"2026-09-07 11:00:00","value":0.40},
                    {"forecastTime":"2026-09-07 14:00:00","value":0.20}],
                  "statistics":{"temperatureMax":21.10,"temperatureMin":19.00,
                                "temperatureAvg":20.10,"precipitationTotal":0.60}}}
                """, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "2026-09-07T08:00:00", "2026-09-07 08:00:00Z",
            "2026-09-07 08:00:00+08:00", "2026-02-30 08:00:00", "2026-02-29 08:00:00",
            "2026-09-07 24:00:00", "2026-9-07 08:00:00", "2026-09-07 08:00",
            "2026-09-07 08:00:00.123", "2026-09-07 08:00:00 "})
    void invalidDateAtEitherBoundaryReturns400(String invalid) throws Exception {
        for (var name : List.of("startTime", "endTime")) {
            error(request().with(r -> { r.setParameter(name, invalid); return r; }), 400);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "1.2", "0", "-1", "9007199254740992", "9223372036854775808"})
    void invalidIdReturns400(String invalid) throws Exception {
        for (var name : List.of("cityId", "modelId")) {
            error(request().with(r -> { r.setParameter(name, invalid); return r; }), 400);
        }
    }

    @Test
    void reversedRangeIs400() throws Exception {
        error(request().with(r -> { r.setParameter("startTime", "2026-09-07 15:00:00"); return r; }), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"cityId", "modelId"})
    void missingAssociationIs404(String name) throws Exception {
        error(request().with(r -> { r.setParameter(name, "999"); return r; }), 404);
    }

    @Test
    void unsupportedExistingCityOrModelIs400() throws Exception {
        var city = DictionaryTestData.cities().get(1);
        when(cities.selectById(8L)).thenReturn(city); error(request(), 400);
        when(cities.selectById(8L)).thenReturn(DictionaryTestData.cities().get(0));
        when(models.selectById(41L)).thenReturn(DictionaryTestData.models().get(1)); error(request(), 400);
    }

    @Test
    void emptyReturnsMetadataEmptyArraysAndFourExplicitNulls() throws Exception {
        when(mapper.selectTrendData(anyLong(), anyLong(), anyList(), any(), any())).thenReturn(List.of());
        mvc.perform(request()).andExpect(status().isOk()).andExpect(content().json("""
                {"code":200,"message":"success","data":{
                  "cityId":8,"cityName":"济南","modelId":41,"modelName":"数据库模型名称",
                  "temperature":[],"precipitation":[],"statistics":{
                    "temperatureMax":null,"temperatureMin":null,"temperatureAvg":null,"precipitationTotal":null}}}
                """, true));
    }

    @Test
    void coreElementMissingIs500() throws Exception {
        when(elements.selectList(any())).thenReturn(List.of(DictionaryTestData.elements().get(0)));
        error(request(), 500);
    }

    @Test
    void serverFailureDoesNotExposeRawDatabaseError() throws Exception {
        when(cities.selectById(8L)).thenThrow(new IllegalStateException("private database details"));
        mvc.perform(request()).andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"code\":500,\"message\":\"internal server error\",\"data\":null}", true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cityId", "modelId", "startTime", "endTime"})
    void missingParameterReturns400InsteadOfMissingRoute(String missing) throws Exception {
        var request = get("/api/weather/trend").param("cityId", "8").param("modelId", "41")
                .param("startTime", "2026-09-07 08:00:00").param("endTime", "2026-09-07 14:00:00");
        request.with(r -> { r.removeParameter(missing); return r; });
        mvc.perform(request).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
    }
}
