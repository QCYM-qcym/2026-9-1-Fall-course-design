package com.shandong.weather;

import com.shandong.weather.controller.WeatherQueryController;
import com.shandong.weather.mapper.ForecastRecordMapper;
import com.shandong.weather.service.WeatherQueryService;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.shandong.weather.WorkbenchTestData.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeatherQueryController.class)
@Import(WeatherQueryService.class)
class WeatherQueryControllerTest {
    @Autowired
    private MockMvc mvc;

    // Keep the controller and service real; only the database boundary is replaced.
    @MockBean
    private ForecastRecordMapper mapper;
    @MockBean private com.shandong.weather.mapper.CityMapper cities;
    @MockBean private com.shandong.weather.mapper.ForecastModelMapper models;
    @MockBean private com.shandong.weather.mapper.WeatherElementMapper elements;

    @BeforeEach
    void setUp() {
        when(mapper.selectWorkbenchModel(41L)).thenReturn(MODEL);
        when(mapper.selectWorkbenchElement(71L)).thenReturn(ELEMENT);
        when(mapper.selectWorkbenchData(eq(41L), eq(71L), eq(START), eq(END), anyList()))
                .thenReturn(List.of(row(8, "济南", START, "19.00"),
                        row(9, "青岛", START, "19.70"), row(8, "济南", END, "21.10")));
    }

    private MockHttpServletRequestBuilder request() {
        return get("/api/weather/workbench")
                .param("modelId", "41").param("elementId", "71")
                .param("startTime", "2026-09-07 08:00:00").param("endTime", "2026-09-07 14:00:00");
    }

    private void error(MockHttpServletRequestBuilder request, int status) throws Exception {
        mvc.perform(request).andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(status))
                .andExpect(jsonPath("$.message", not(emptyOrNullString())))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void normalRequestReturnsOnlyFrozenFieldsAndLocalTimes() throws Exception {
        mvc.perform(request()).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data", aMapWithSize(4)))
                .andExpect(jsonPath("$.data.model", aMapWithSize(3)))
                .andExpect(jsonPath("$.data.model.id").value(41))
                .andExpect(jsonPath("$.data.model.modelCode").value("ECMWF"))
                .andExpect(jsonPath("$.data.model.modelName").value("Database model name"))
                .andExpect(jsonPath("$.data.element", aMapWithSize(4)))
                .andExpect(jsonPath("$.data.element.id").value(71))
                .andExpect(jsonPath("$.data.element.elementCode").value("T2M"))
                .andExpect(jsonPath("$.data.element.elementName").value("2 米气温"))
                .andExpect(jsonPath("$.data.element.unit").value("℃"))
                .andExpect(jsonPath("$.data.times", contains("2026-09-07 08:00:00", "2026-09-07 14:00:00")))
                .andExpect(jsonPath("$.data.records", hasSize(3)))
                .andExpect(jsonPath("$.data.records[0]", aMapWithSize(4)))
                .andExpect(jsonPath("$.data.records[0].cityId").value(8))
                .andExpect(jsonPath("$.data.records[0].cityName").value("济南"))
                .andExpect(jsonPath("$.data.records[0].forecastTime").value("2026-09-07 08:00:00"))
                .andExpect(jsonPath("$.data.records[0].value").value(19.00));
    }

    @ParameterizedTest
    @ValueSource(strings = {"modelId", "elementId", "startTime", "endTime"})
    void missingRequiredParameterReturns400(String missing) throws Exception {
        var req = request();
        req.with(r -> { r.removeParameter(missing); return r; });
        error(req, 400);
        verify(mapper, never()).selectWorkbenchData(anyLong(), anyLong(), any(), any(), anyList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "2026-09-07T08:00:00", "2026-09-07 08:00:00Z",
            "2026-09-07 08:00:00+08:00", "2026-02-30 08:00:00", "2026-02-29 08:00:00",
            "2026-09-07 24:00:00", "2026-9-07 08:00:00", "2026-09-07 08:00",
            "2026-09-07 08:00:00.123", "2026-09-07 08:00:00 "})
    void malformedDateReturns400ForEitherEndpoint(String invalid) throws Exception {
        error(request().with(r -> { r.setParameter("startTime", invalid); return r; }), 400);
        error(request().with(r -> { r.setParameter("endTime", invalid); return r; }), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "1.2", "0", "-1", "9007199254740992", "9223372036854775808"})
    void invalidIdReturns400ForEitherId(String invalid) throws Exception {
        error(request().with(r -> { r.setParameter("modelId", invalid); return r; }), 400);
        error(request().with(r -> { r.setParameter("elementId", invalid); return r; }), 400);
    }

    @Test
    void reversedRangeReturns400() throws Exception {
        error(request().with(r -> { r.setParameter("startTime", "2026-09-07 15:00:00"); return r; }), 400);
    }

    @Test
    void nonexistentModelReturns404() throws Exception {
        error(request().with(r -> { r.setParameter("modelId", "999"); return r; }), 404);
    }

    @Test
    void nonexistentElementReturns404() throws Exception {
        error(request().with(r -> { r.setParameter("elementId", "999"); return r; }), 404);
    }

    @Test
    void noMatchingRecordsReturns200AndMetadataWithEmptyArrays() throws Exception {
        mvc.perform(request().with(r -> { r.setParameter("startTime", "2027-01-01 00:00:00");
                    r.setParameter("endTime", "2027-01-02 00:00:00"); return r; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.model.id").value(41))
                .andExpect(jsonPath("$.data.element.id").value(71))
                .andExpect(jsonPath("$.data.times", empty()))
                .andExpect(jsonPath("$.data.records", empty()));
    }

    @Test
    void unexpectedFailureReturns500WithoutLeakingDatabaseDetails() throws Exception {
        when(mapper.selectWorkbenchModel(41L)).thenThrow(new IllegalStateException("private SQL details"));
        mvc.perform(request()).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("internal server error"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
