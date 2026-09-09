package com.shandong.weather;

import com.shandong.weather.controller.WeatherElementController;
import com.shandong.weather.service.WeatherElementService;
import com.shandong.weather.mapper.WeatherElementMapper;
import com.shandong.weather.mapper.ForecastRecordMapper;
import com.shandong.weather.entity.WeatherElement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeatherElementController.class)
@Import(WeatherElementService.class)
class WeatherElementCrudControllerTest {
    @Test void rejectsOverlongFields() throws Exception {
        for (String body : new String[]{BODY.replace("TEMP_ELEMENT","X".repeat(33)), BODY.replace("Test element","X".repeat(51)), BODY.replace("mm","X".repeat(17))})
            mvc.perform(post("/api/weather-elements").contentType("application/json").content(body)).andExpect(status().isBadRequest());
    }
    @org.junit.jupiter.api.Test void updateLocksDictionaryBeforeReferenceCheck() throws Exception {
        var old = new com.shandong.weather.entity.WeatherElement(); old.setId(80L); old.setElementCode("TEMP_ELEMENT"); old.setElementName("Test"); old.setUnit("mm");
        when(mapper.selectOne(any())).thenReturn(old);
        when(mapper.updateById(any(com.shandong.weather.entity.WeatherElement.class))).thenReturn(1);
        mvc.perform(put("/api/weather-elements/80").contentType("application/json").content(BODY)).andExpect(status().isOk());
        verify(mapper).selectOne(argThat(q -> q.getSqlSegment().contains("FOR UPDATE")));
    }
    @Autowired MockMvc mvc;
    @MockBean WeatherElementMapper mapper;
    @MockBean ForecastRecordMapper records;
    static final String BODY = "{\"elementCode\":\"TEMP_ELEMENT\",\"elementName\":\"Test element\",\"unit\":\"mm\"}";
    @Test void createsWeatherElementWithGeneratedId() throws Exception {
        when(mapper.insert(any(WeatherElement.class))).thenAnswer(inv -> { inv.<WeatherElement>getArgument(0).setId(80L); return 1; });
        mvc.perform(post("/api/weather-elements").contentType("application/json").content(BODY))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(80))
            .andExpect(jsonPath("$.data.elementCode").value("TEMP_ELEMENT"));
    }
    @Test void rejectsMissingFields() throws Exception {
        mvc.perform(post("/api/weather-elements").contentType("application/json").content("{}")).andExpect(status().isBadRequest());
    }
    @Test void rejectsInvalidJson() throws Exception {
        mvc.perform(post("/api/weather-elements").contentType("application/json").content("{")).andExpect(status().isBadRequest());
    }
    @Test void updatesExistingWeatherElement() throws Exception {
        WeatherElement old = new WeatherElement(); old.setId(80L); old.setElementCode("TEMP_ELEMENT"); old.setUnit("mm");
        when(mapper.selectOne(any())).thenReturn(old);
        when(mapper.updateById(any(WeatherElement.class))).thenReturn(1);
        mvc.perform(put("/api/weather-elements/80").contentType("application/json").content(BODY))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.elementName").value("Test element"));
    }
    @Test void missingUpdateIs404() throws Exception {
        mvc.perform(put("/api/weather-elements/80").contentType("application/json").content(BODY)).andExpect(status().isNotFound());
    }
    @Test void deletesExistingWeatherElement() throws Exception {
        WeatherElement old = new WeatherElement(); old.setId(80L);
        when(mapper.selectOne(any())).thenReturn(old); when(mapper.deleteById(80L)).thenReturn(1);
        mvc.perform(delete("/api/weather-elements/80")).andExpect(status().isOk()).andExpect(content().json("{\"code\":200,\"message\":\"success\",\"data\":null}"));
    }
    @Test void missingDeleteIs404() throws Exception {
        mvc.perform(delete("/api/weather-elements/80")).andExpect(status().isNotFound());
    }
    @Test void duplicateCodeIs409BeforeInsert() throws Exception {
        when(mapper.selectCount(any())).thenReturn(1L);
        mvc.perform(post("/api/weather-elements").contentType("application/json").content(BODY)).andExpect(status().isConflict());
        verify(mapper, never()).insert(any(WeatherElement.class));
    }
    @Test void updateDuplicateExcludesOwnId() throws Exception {
        WeatherElement old = new WeatherElement(); old.setId(80L); old.setElementCode("TEMP_ELEMENT"); old.setUnit("mm");
        when(mapper.selectOne(any())).thenReturn(old);
        when(mapper.selectCount(any())).thenAnswer(inv -> {
            var wrapper = (com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?>) inv.getArgument(0);
            org.assertj.core.api.Assertions.assertThat(wrapper.getSqlSegment()).contains("element_code =", "id <>");
            org.assertj.core.api.Assertions.assertThat(wrapper.getParamNameValuePairs().values()).contains("TEMP_ELEMENT", 80L);
            return 1L;
        });
        mvc.perform(put("/api/weather-elements/80").contentType("application/json").content(BODY)).andExpect(status().isConflict());
        verify(mapper, never()).updateById(any(WeatherElement.class));
    }
    @Test void referencedDeleteIs409() throws Exception {
        WeatherElement old = new WeatherElement(); old.setId(80L);
        when(mapper.selectOne(any())).thenReturn(old); when(records.selectCount(any())).thenReturn(1L);
        mvc.perform(delete("/api/weather-elements/80")).andExpect(status().isConflict());
        verify(mapper, never()).deleteById(80L);
    }
    @Test void referencedCodeChangeIs409() throws Exception {
        WeatherElement old = new WeatherElement(); old.setId(80L); old.setElementCode("PRECIP");
        when(mapper.selectOne(any())).thenReturn(old); when(records.selectCount(any())).thenReturn(1L);
        mvc.perform(put("/api/weather-elements/80").contentType("application/json").content(BODY)).andExpect(status().isConflict());
    }
    @Test void referencedDisplayUpdateAllowed() throws Exception {
        WeatherElement old = new WeatherElement(); old.setId(80L); old.setElementCode("TEMP_ELEMENT"); old.setUnit("mm");
        when(mapper.selectOne(any())).thenReturn(old); when(records.selectCount(any())).thenReturn(1L);
        when(mapper.updateById(any(WeatherElement.class))).thenReturn(1);
        mvc.perform(put("/api/weather-elements/80").contentType("application/json").content(BODY)).andExpect(status().isOk());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"0", "-1", "9007199254740992", "abc"})
    void invalidPathIdIs400(String id) throws Exception {
        mvc.perform(delete("/api/weather-elements/" + id)).andExpect(status().isBadRequest());
    }
    @Test void databaseDuplicateFallbackIs409() throws Exception {
        when(mapper.insert(any(WeatherElement.class))).thenThrow(new org.springframework.dao.DuplicateKeyException("private SQL"));
        mvc.perform(post("/api/weather-elements").contentType("application/json").content(BODY)).andExpect(status().isConflict());
    }
    @Test void unknownDatabaseErrorIsSafe500() throws Exception {
        when(mapper.insert(any(WeatherElement.class))).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private connection details"));
        mvc.perform(post("/api/weather-elements").contentType("application/json").content(BODY)).andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("internal server error"));
    }
    @Test void referencedUnitChangeIs409() throws Exception {
        WeatherElement old = new WeatherElement(); old.setId(80L); old.setElementCode("TEMP_ELEMENT"); old.setUnit("mm"); old.setUnit("kg");
        when(mapper.selectOne(any())).thenReturn(old); when(records.selectCount(any())).thenReturn(1L);
        mvc.perform(put("/api/weather-elements/80").contentType("application/json").content(BODY)).andExpect(status().isConflict());
    }
    @Test void invalidCoreUnitIs400() throws Exception {
        mvc.perform(post("/api/weather-elements").contentType("application/json").content(BODY.replace("TEMP_ELEMENT","T2M"))).andExpect(status().isBadRequest());
    }
}
