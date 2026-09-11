package com.shandong.weather;

import com.shandong.weather.controller.WeatherQueryController;
import com.shandong.weather.service.WeatherQueryService;
import com.shandong.weather.mapper.*;
import com.shandong.weather.entity.ForecastModel;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import static com.shandong.weather.WorkbenchTestData.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeatherQueryController.class)
@Import(WeatherQueryService.class)
class WeatherComparisonControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private ForecastRecordMapper records;
    @MockBean private CityMapper cities;
    @MockBean private WeatherElementMapper elements;
    @MockBean private ForecastModelMapper models;

    @BeforeEach
    void setUp() {
        when(cities.selectById(8L)).thenReturn(DictionaryTestData.cities().get(0));
        when(elements.selectById(71L)).thenReturn(DictionaryTestData.elements().get(0));
        var noaa = new ForecastModel();
        noaa.setId(59L); noaa.setModelCode("NOAA"); noaa.setModelName("NOAA database name");
        when(models.selectList(any())).thenReturn(List.of(noaa, DictionaryTestData.models().get(0)));
        when(records.selectComparisonData(8L, 71L, List.of(41L, 59L), START, END)).thenReturn(List.of(
                new ComparisonRecordRow("NOAA", START, new BigDecimal("18.40")),
                new ComparisonRecordRow("ECMWF", START, new BigDecimal("19.00"))));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request() {
        return get("/api/weather/comparison").param("cityId", "8").param("elementId", "71")
                .param("startTime", "2026-09-07 08:00:00").param("endTime", "2026-09-07 14:00:00");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"TCC,%", "WIND_SPEED_100M,m/s", "WIND_DIR_100M,°", "RH,%"})
    void monthlyElementsCompareBothModelsWithDictionaryUnits(String code, String unit) throws Exception {
        var element = DictionaryTestData.elements().get(0);
        element.setElementCode(code); element.setElementName(code); element.setUnit(unit);
        when(elements.selectById(71L)).thenReturn(element);
        mvc.perform(request()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.element.elementCode").value(code))
                .andExpect(jsonPath("$.data.element.unit").value(unit))
                .andExpect(jsonPath("$.data.series", org.hamcrest.Matchers.hasSize(2)));
    }

    private void error(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, int code)
            throws Exception {
        mvc.perform(request).andExpect(status().is(code)).andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void exactResponseHasNestedElementAndTwoOrderedSeriesWithoutTrendFields() throws Exception {
        mvc.perform(request()).andExpect(status().isOk()).andExpect(content().json("""
                {"code":200,"message":"success","data":{
                  "cityId":8,"cityName":"济南",
                  "element":{"id":71,"elementCode":"T2M","elementName":"2 米气温","unit":"℃"},
                  "series":[
                    {"modelId":41,"modelCode":"ECMWF","modelName":"数据库模型名称",
                     "values":[{"forecastTime":"2026-09-07 08:00:00","value":19.00}]},
                    {"modelId":59,"modelCode":"NOAA","modelName":"NOAA database name",
                     "values":[{"forecastTime":"2026-09-07 08:00:00","value":18.40}]}]}}
                """, true));
    }

    @Test
    void emptyRetainsBothSeriesAndMetadata() throws Exception {
        when(records.selectComparisonData(anyLong(), anyLong(), anyList(), any(), any())).thenReturn(List.of());
        mvc.perform(request()).andExpect(status().isOk()).andExpect(content().json("""
                {"code":200,"message":"success","data":{
                  "cityId":8,"cityName":"济南",
                  "element":{"id":71,"elementCode":"T2M","elementName":"2 米气温","unit":"℃"},
                  "series":[
                    {"modelId":41,"modelCode":"ECMWF","modelName":"数据库模型名称","values":[]},
                    {"modelId":59,"modelCode":"NOAA","modelName":"NOAA database name","values":[]}]}}
                """, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "2026-09-07T08:00:00", "2026-09-07 08:00:00Z",
            "2026-09-07 08:00:00+08:00", "2026-02-30 08:00:00", "2026-02-29 08:00:00",
            "2026-09-07 24:00:00", "2026-9-07 08:00:00", "2026-09-07 08:00",
            "2026-09-07 08:00:00.123", "2026-09-07 08:00:00 "})
    void invalidDateAtEitherBoundaryIs400(String invalid) throws Exception {
        for (var name : List.of("startTime", "endTime")) {
            error(request().with(r -> { r.setParameter(name, invalid); return r; }), 400);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "1.2", "0", "-1", "9007199254740992", "9223372036854775808"})
    void invalidIdIs400(String invalid) throws Exception {
        for (var name : List.of("cityId", "elementId")) {
            error(request().with(r -> { r.setParameter(name, invalid); return r; }), 400);
        }
    }

    @Test
    void reversedTimeIs400() throws Exception {
        error(request().with(r -> { r.setParameter("startTime", "2026-09-07 15:00:00"); return r; }), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"cityId", "elementId"})
    void nonexistentAssociationIs404(String name) throws Exception {
        error(request().with(r -> { r.setParameter(name, "999"); return r; }), 404);
    }

    @Test
    void unsupportedCityOrElementIs400() throws Exception {
        when(cities.selectById(8L)).thenReturn(DictionaryTestData.cities().get(1)); error(request(), 400);
        when(cities.selectById(8L)).thenReturn(DictionaryTestData.cities().get(0));
        when(elements.selectById(71L)).thenReturn(DictionaryTestData.elements().get(1)); error(request(), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ECMWF", "NOAA"})
    void missingCoreModelIs500(String missing) throws Exception {
        var remaining = new ForecastModel(); remaining.setId(41L);
        remaining.setModelCode("ECMWF".equals(missing) ? "NOAA" : "ECMWF");
        remaining.setModelName("remaining");
        when(models.selectList(any())).thenReturn(List.of(remaining)); error(request(), 500);
    }

    @Test
    void rawDatabaseFailureIsNotExposed() throws Exception {
        when(cities.selectById(8L)).thenThrow(new IllegalStateException("private database details"));
        mvc.perform(request()).andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"code\":500,\"message\":\"internal server error\",\"data\":null}", true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cityId", "elementId", "startTime", "endTime"})
    void missingRequiredParameterIs400NotMissingRoute(String missing) throws Exception {
        var request = get("/api/weather/comparison").param("cityId", "8").param("elementId", "71")
                .param("startTime", "2026-09-07 08:00:00").param("endTime", "2026-09-07 14:00:00");
        request.with(r -> { r.removeParameter(missing); return r; });
        mvc.perform(request).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
    }
}
