package com.shandong.weather;
import com.shandong.weather.entity.*;
import com.shandong.weather.mapper.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(com.shandong.weather.controller.ForecastRecordController.class)
@org.springframework.context.annotation.Import(com.shandong.weather.service.ForecastRecordService.class)
class ForecastRecordCrudControllerTest {
    @Autowired MockMvc mvc;
    @Autowired com.shandong.weather.service.ForecastRecordService service;
    @MockBean ForecastRecordMapper records;
    @MockBean CityMapper cities;
    @MockBean ForecastModelMapper models;
    @MockBean WeatherElementMapper elements;
    static final String BODY = "{\"cityId\":8,\"modelId\":41,\"elementId\":71,\"forecastTime\":\"2026-09-07 08:00:00\",\"value\":0.00}";
    City city; ForecastModel model; WeatherElement element;
    ForecastRecord row() {
        var r = new ForecastRecord(); r.setId(90L); r.setCityId(8L); r.setModelId(41L); r.setElementId(71L);
        r.setForecastTime(LocalDateTime.of(2026,9,7,8,0)); r.setValue(BigDecimal.ZERO); return r;
    }
    @BeforeEach void dictionaries() {
        city = new City(); city.setId(8L); city.setCityCode("JINAN"); city.setCityName("济南");
        model = new ForecastModel(); model.setId(41L); model.setModelCode("ECMWF"); model.setModelName("ECMWF");
        element = new WeatherElement(); element.setId(71L); element.setElementCode("PRECIP"); element.setElementName("降水量"); element.setUnit("mm");
        when(cities.selectOne(any())).thenReturn(city); when(models.selectOne(any())).thenReturn(model); when(elements.selectOne(any())).thenReturn(element);
        when(cities.selectBatchIds(anyCollection())).thenReturn(List.of(city));
        when(models.selectBatchIds(anyCollection())).thenReturn(List.of(model));
        when(elements.selectBatchIds(anyCollection())).thenReturn(List.of(element));
    }
    @Test void getAllRequestsIdOrderAndExpandedMetadata() throws Exception {
        when(records.selectList(any())).thenAnswer(inv -> {
            var q = (com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?>) inv.getArgument(0);
            assertThat(q.getSqlSegment().trim()).isEqualTo("ORDER BY id ASC"); return List.of(row());
        });
        assertThat(service.listAll()).hasSize(1);
        mvc.perform(get("/api/forecast-records")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].cityName").value("济南")).andExpect(jsonPath("$.data[0].modelCode").value("ECMWF"))
            .andExpect(jsonPath("$.data[0].unit").value("mm")).andExpect(jsonPath("$.data[0].forecastTime").value("2026-09-07 08:00:00"));
    }
    @Test void emptyGetIs200Array() throws Exception {
        when(records.selectList(any())).thenReturn(List.of());
        mvc.perform(get("/api/forecast-records")).andExpect(status().isOk()).andExpect(content().json("{\"code\":200,\"message\":\"success\",\"data\":[]}"));
    }
    @Test void createsRealZeroAndReturnsExpandedVo() throws Exception {
        when(records.insert(any(ForecastRecord.class))).thenAnswer(inv -> { inv.<ForecastRecord>getArgument(0).setId(90L); return 1; });
        mvc.perform(post("/api/forecast-records").contentType("application/json").content(BODY)).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(90)).andExpect(jsonPath("$.data.value").value(0))
            .andExpect(jsonPath("$.data.elementCode").value("PRECIP"));
        var locks = inOrder(cities, models, elements, records);
        locks.verify(cities).selectOne(argThat(q -> q.getSqlSegment().contains("FOR UPDATE")));
        locks.verify(models).selectOne(argThat(q -> q.getSqlSegment().contains("FOR UPDATE")));
        locks.verify(elements).selectOne(argThat(q -> q.getSqlSegment().contains("FOR UPDATE")));
        locks.verify(records).insert(any(ForecastRecord.class));
    }
    @ParameterizedTest @ValueSource(strings={"city","model","element"})
    void missingAssociationIs404(String missing) throws Exception {
        if (missing.equals("city")) when(cities.selectOne(any())).thenReturn(null);
        if (missing.equals("model")) when(models.selectOne(any())).thenReturn(null);
        if (missing.equals("element")) when(elements.selectOne(any())).thenReturn(null);
        mvc.perform(post("/api/forecast-records").contentType("application/json").content(BODY)).andExpect(status().isNotFound());
        verify(records,never()).insert(any(ForecastRecord.class));
    }
    @Test void duplicateCreateIs409() throws Exception {
        when(records.selectCount(any())).thenReturn(1L);
        mvc.perform(post("/api/forecast-records").contentType("application/json").content(BODY)).andExpect(status().isConflict());
    }
    @Test void negativePrecipitationIs400() throws Exception {
        mvc.perform(post("/api/forecast-records").contentType("application/json").content(BODY.replace("0.00","-0.01"))).andExpect(status().isBadRequest());
    }
    @Test void temperatureHasNoArtificialLimit() throws Exception {
        element.setElementCode("T2M"); element.setUnit("℃");
        when(records.insert(any(ForecastRecord.class))).thenAnswer(inv -> { inv.<ForecastRecord>getArgument(0).setId(90L); return 1; });
        mvc.perform(post("/api/forecast-records").contentType("application/json").content(BODY.replace("0.00","99.99"))).andExpect(status().isOk());
    }
    @Test void updatesRecord() throws Exception {
        when(records.selectById(90L)).thenReturn(row()); when(records.updateById(any(ForecastRecord.class))).thenReturn(1);
        mvc.perform(put("/api/forecast-records/90").contentType("application/json").content(BODY)).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(90));
    }
    @Test void missingUpdateIs404() throws Exception {
        mvc.perform(put("/api/forecast-records/90").contentType("application/json").content(BODY)).andExpect(status().isNotFound());
    }
    @Test void updateDuplicateExcludesSelf() throws Exception {
        when(records.selectById(90L)).thenReturn(row());
        when(records.selectCount(any())).thenAnswer(inv -> {
            var q = (com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?>) inv.getArgument(0);
            assertThat(q.getSqlSegment()).contains("city_id =", "model_id =", "element_id =", "forecast_time =", "id <>");
            assertThat(q.getParamNameValuePairs().values()).contains(8L,41L,71L,90L,LocalDateTime.of(2026,9,7,8,0));
            return 1L;
        });
        mvc.perform(put("/api/forecast-records/90").contentType("application/json").content(BODY)).andExpect(status().isConflict());
    }
    @Test void deletesRecord() throws Exception {
        when(records.selectById(90L)).thenReturn(row()); when(records.deleteById(90L)).thenReturn(1);
        mvc.perform(delete("/api/forecast-records/90")).andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
    }
    @Test void missingDeleteIs404() throws Exception { mvc.perform(delete("/api/forecast-records/90")).andExpect(status().isNotFound()); }
    @ParameterizedTest @ValueSource(strings={"2026-02-30 08:00:00","2026-09-07T08:00:00","2026-09-07 08:00:00Z","2026-09-07 08:00"})
    void invalidTimeIs400(String time) throws Exception {
        mvc.perform(post("/api/forecast-records").contentType("application/json").content(BODY.replace("2026-09-07 08:00:00",time))).andExpect(status().isBadRequest());
    }
    @ParameterizedTest @ValueSource(strings={"0.001","100000000.00","-100000000.00","null"})
    void invalidValueIs400(String value) throws Exception {
        mvc.perform(post("/api/forecast-records").contentType("application/json").content(BODY.replace("0.00",value))).andExpect(status().isBadRequest());
    }
    @Test void missingBodyFieldsIs400() throws Exception {
        mvc.perform(post("/api/forecast-records").contentType("application/json").content("{}")).andExpect(status().isBadRequest());
    }
    @ParameterizedTest @ValueSource(strings={"8.9","\"8\"","true","0","9007199254740992"})
    void invalidAssociationIdIs400(String id) throws Exception {
        mvc.perform(post("/api/forecast-records").contentType("application/json").content(BODY.replace("\"cityId\":8","\"cityId\":" + id)))
                .andExpect(status().isBadRequest());
        verify(records,never()).insert(any(ForecastRecord.class));
    }
}
