package com.shandong.weather;

import com.shandong.weather.controller.ForecastModelController;
import com.shandong.weather.service.ForecastModelService;
import com.shandong.weather.mapper.ForecastModelMapper;
import com.shandong.weather.mapper.ForecastRecordMapper;
import com.shandong.weather.entity.ForecastModel;
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

@WebMvcTest(ForecastModelController.class)
@Import(ForecastModelService.class)
@BusinessSecurityTest
@org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
class ForecastModelCrudControllerTest {
    @Test void rejectsOverlongFields() throws Exception {
        for (String body : new String[]{BODY.replace("TEMP_MODEL","X".repeat(33)), BODY.replace("Test model","X".repeat(51)),
                BODY.replace("\"description\":\"description\"","\"description\":\"" + "X".repeat(256) + "\"")})
            mvc.perform(post("/api/forecast-models").contentType("application/json").content(body)).andExpect(status().isBadRequest());
    }
    @org.junit.jupiter.api.Test void updateLocksDictionaryBeforeReferenceCheck() throws Exception {
        var old = new com.shandong.weather.entity.ForecastModel(); old.setId(80L); old.setModelCode("TEMP_MODEL"); old.setModelName("Test");
        when(mapper.selectOne(any())).thenReturn(old);
        when(mapper.updateById(any(com.shandong.weather.entity.ForecastModel.class))).thenReturn(1);
        mvc.perform(put("/api/forecast-models/80").contentType("application/json").content(BODY)).andExpect(status().isOk());
        verify(mapper).selectOne(argThat(q -> q.getSqlSegment().contains("FOR UPDATE")));
    }
    @Autowired MockMvc mvc;
    @MockBean ForecastModelMapper mapper;
    @MockBean ForecastRecordMapper records;
    static final String BODY = "{\"modelCode\":\"TEMP_MODEL\",\"modelName\":\"Test model\",\"description\":\"description\"}";
    @Test void createsForecastModelWithGeneratedId() throws Exception {
        when(mapper.insert(any(ForecastModel.class))).thenAnswer(inv -> { inv.<ForecastModel>getArgument(0).setId(80L); return 1; });
        mvc.perform(post("/api/forecast-models").contentType("application/json").content(BODY))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(80))
            .andExpect(jsonPath("$.data.modelCode").value("TEMP_MODEL"));
    }
    @Test void rejectsMissingFields() throws Exception {
        mvc.perform(post("/api/forecast-models").contentType("application/json").content("{}")).andExpect(status().isBadRequest());
    }
    @Test void rejectsInvalidJson() throws Exception {
        mvc.perform(post("/api/forecast-models").contentType("application/json").content("{")).andExpect(status().isBadRequest());
    }
    @Test void updatesExistingForecastModel() throws Exception {
        ForecastModel old = new ForecastModel(); old.setId(80L); old.setModelCode("TEMP_MODEL");
        when(mapper.selectOne(any())).thenReturn(old);
        when(mapper.updateById(any(ForecastModel.class))).thenReturn(1);
        mvc.perform(put("/api/forecast-models/80").contentType("application/json").content(BODY))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.modelName").value("Test model"));
    }
    @Test void missingUpdateIs404() throws Exception {
        mvc.perform(put("/api/forecast-models/80").contentType("application/json").content(BODY)).andExpect(status().isNotFound());
    }
    @Test void deletesExistingForecastModel() throws Exception {
        ForecastModel old = new ForecastModel(); old.setId(80L);
        when(mapper.selectOne(any())).thenReturn(old); when(mapper.deleteById(80L)).thenReturn(1);
        mvc.perform(delete("/api/forecast-models/80")).andExpect(status().isOk()).andExpect(content().json("{\"code\":200,\"message\":\"success\",\"data\":null}"));
    }
    @Test void missingDeleteIs404() throws Exception {
        mvc.perform(delete("/api/forecast-models/80")).andExpect(status().isNotFound());
    }
    @Test void duplicateCodeIs409BeforeInsert() throws Exception {
        when(mapper.selectCount(any())).thenReturn(1L);
        mvc.perform(post("/api/forecast-models").contentType("application/json").content(BODY)).andExpect(status().isConflict());
        verify(mapper, never()).insert(any(ForecastModel.class));
    }
    @Test void updateDuplicateExcludesOwnId() throws Exception {
        ForecastModel old = new ForecastModel(); old.setId(80L); old.setModelCode("TEMP_MODEL");
        when(mapper.selectOne(any())).thenReturn(old);
        when(mapper.selectCount(any())).thenAnswer(inv -> {
            var wrapper = (com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?>) inv.getArgument(0);
            org.assertj.core.api.Assertions.assertThat(wrapper.getSqlSegment()).contains("model_code =", "id <>");
            org.assertj.core.api.Assertions.assertThat(wrapper.getParamNameValuePairs().values()).contains("TEMP_MODEL", 80L);
            return 1L;
        });
        mvc.perform(put("/api/forecast-models/80").contentType("application/json").content(BODY)).andExpect(status().isConflict());
        verify(mapper, never()).updateById(any(ForecastModel.class));
    }
    @Test void referencedDeleteIs409() throws Exception {
        ForecastModel old = new ForecastModel(); old.setId(80L);
        when(mapper.selectOne(any())).thenReturn(old); when(records.selectCount(any())).thenReturn(1L);
        mvc.perform(delete("/api/forecast-models/80")).andExpect(status().isConflict());
        verify(mapper, never()).deleteById(80L);
    }
    @Test void referencedCodeChangeIs409() throws Exception {
        ForecastModel old = new ForecastModel(); old.setId(80L); old.setModelCode("ECMWF");
        when(mapper.selectOne(any())).thenReturn(old); when(records.selectCount(any())).thenReturn(1L);
        mvc.perform(put("/api/forecast-models/80").contentType("application/json").content(BODY)).andExpect(status().isConflict());
    }
    @Test void referencedDisplayUpdateAllowed() throws Exception {
        ForecastModel old = new ForecastModel(); old.setId(80L); old.setModelCode("TEMP_MODEL");
        when(mapper.selectOne(any())).thenReturn(old); when(records.selectCount(any())).thenReturn(1L);
        when(mapper.updateById(any(ForecastModel.class))).thenReturn(1);
        mvc.perform(put("/api/forecast-models/80").contentType("application/json").content(BODY)).andExpect(status().isOk());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"0", "-1", "9007199254740992", "abc"})
    void invalidPathIdIs400(String id) throws Exception {
        mvc.perform(delete("/api/forecast-models/" + id)).andExpect(status().isBadRequest());
    }
    @Test void databaseDuplicateFallbackIs409() throws Exception {
        when(mapper.insert(any(ForecastModel.class))).thenThrow(new org.springframework.dao.DuplicateKeyException("private SQL"));
        mvc.perform(post("/api/forecast-models").contentType("application/json").content(BODY)).andExpect(status().isConflict());
    }
    @Test void unknownDatabaseErrorIsSafe500() throws Exception {
        when(mapper.insert(any(ForecastModel.class))).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private connection details"));
        mvc.perform(post("/api/forecast-models").contentType("application/json").content(BODY)).andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("internal server error"));
    }
}
