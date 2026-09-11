package com.shandong.weather;

import com.shandong.weather.controller.CityController;
import com.shandong.weather.service.CityService;
import com.shandong.weather.mapper.CityMapper;
import com.shandong.weather.mapper.ForecastRecordMapper;
import com.shandong.weather.entity.City;
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

@WebMvcTest(CityController.class)
@Import(CityService.class)
@BusinessSecurityTest
@org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
class CityCrudControllerTest {
    @org.junit.jupiter.api.Test void updateLocksDictionaryBeforeReferenceCheck() throws Exception {
        var old = new com.shandong.weather.entity.City(); old.setId(80L); old.setCityCode("TEMP_CITY"); old.setCityName("Test"); old.setLongitude(new java.math.BigDecimal("120.123456")); old.setLatitude(new java.math.BigDecimal("36.5"));
        when(mapper.selectOne(any())).thenReturn(old);
        when(mapper.updateById(any(com.shandong.weather.entity.City.class))).thenReturn(1);
        mvc.perform(put("/api/cities/80").contentType("application/json").content(BODY)).andExpect(status().isOk());
        verify(mapper).selectOne(argThat(q -> q.getSqlSegment().contains("FOR UPDATE")));
    }
    @Autowired MockMvc mvc;
    @MockBean CityMapper mapper;
    @MockBean ForecastRecordMapper records;
    static final String BODY = "{\"cityCode\":\"TEMP_CITY\",\"cityName\":\"Test city\",\"longitude\":120.123456,\"latitude\":36.5}";
    @Test void createsCityWithGeneratedId() throws Exception {
        when(mapper.insert(any(City.class))).thenAnswer(inv -> { inv.<City>getArgument(0).setId(80L); return 1; });
        mvc.perform(post("/api/cities").contentType("application/json").content(BODY))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(80))
            .andExpect(jsonPath("$.data.cityCode").value("TEMP_CITY"));
    }
    @Test void rejectsMissingFields() throws Exception {
        mvc.perform(post("/api/cities").contentType("application/json").content("{}")).andExpect(status().isBadRequest());
    }
    @Test void rejectsInvalidJson() throws Exception {
        mvc.perform(post("/api/cities").contentType("application/json").content("{")).andExpect(status().isBadRequest());
    }
    @Test void updatesExistingCity() throws Exception {
        City old = new City(); old.setId(80L); old.setCityCode("TEMP_CITY");
        when(mapper.selectOne(any())).thenReturn(old);
        when(mapper.updateById(any(City.class))).thenReturn(1);
        mvc.perform(put("/api/cities/80").contentType("application/json").content(BODY))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.cityName").value("Test city"));
    }
    @Test void missingUpdateIs404() throws Exception {
        mvc.perform(put("/api/cities/80").contentType("application/json").content(BODY)).andExpect(status().isNotFound());
    }
    @Test void deletesExistingCity() throws Exception {
        City old = new City(); old.setId(80L);
        when(mapper.selectOne(any())).thenReturn(old); when(mapper.deleteById(80L)).thenReturn(1);
        mvc.perform(delete("/api/cities/80")).andExpect(status().isOk()).andExpect(content().json("{\"code\":200,\"message\":\"success\",\"data\":null}"));
    }
    @Test void missingDeleteIs404() throws Exception {
        mvc.perform(delete("/api/cities/80")).andExpect(status().isNotFound());
    }
    @Test void duplicateCodeIs409BeforeInsert() throws Exception {
        when(mapper.selectCount(any())).thenReturn(1L);
        mvc.perform(post("/api/cities").contentType("application/json").content(BODY)).andExpect(status().isConflict());
        verify(mapper, never()).insert(any(City.class));
    }
    @Test void updateDuplicateExcludesOwnId() throws Exception {
        City old = new City(); old.setId(80L); old.setCityCode("TEMP_CITY");
        when(mapper.selectOne(any())).thenReturn(old);
        when(mapper.selectCount(any())).thenAnswer(inv -> {
            var wrapper = (com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?>) inv.getArgument(0);
            org.assertj.core.api.Assertions.assertThat(wrapper.getSqlSegment()).contains("city_code =", "id <>");
            org.assertj.core.api.Assertions.assertThat(wrapper.getParamNameValuePairs().values()).contains("TEMP_CITY", 80L);
            return 1L;
        });
        mvc.perform(put("/api/cities/80").contentType("application/json").content(BODY)).andExpect(status().isConflict());
        verify(mapper, never()).updateById(any(City.class));
    }
    @Test void referencedDeleteIs409() throws Exception {
        City old = new City(); old.setId(80L);
        when(mapper.selectOne(any())).thenReturn(old); when(records.selectCount(any())).thenReturn(1L);
        mvc.perform(delete("/api/cities/80")).andExpect(status().isConflict());
        verify(mapper, never()).deleteById(80L);
    }
    @Test void referencedCodeChangeIs409() throws Exception {
        City old = new City(); old.setId(80L); old.setCityCode("JINAN");
        when(mapper.selectOne(any())).thenReturn(old); when(records.selectCount(any())).thenReturn(1L);
        mvc.perform(put("/api/cities/80").contentType("application/json").content(BODY)).andExpect(status().isConflict());
    }
    @Test void referencedDisplayUpdateAllowed() throws Exception {
        City old = new City(); old.setId(80L); old.setCityCode("TEMP_CITY");
        when(mapper.selectOne(any())).thenReturn(old); when(records.selectCount(any())).thenReturn(1L);
        when(mapper.updateById(any(City.class))).thenReturn(1);
        mvc.perform(put("/api/cities/80").contentType("application/json").content(BODY)).andExpect(status().isOk());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"0", "-1", "9007199254740992", "abc"})
    void invalidPathIdIs400(String id) throws Exception {
        mvc.perform(delete("/api/cities/" + id)).andExpect(status().isBadRequest());
    }
    @Test void coordinatePrecisionIs400() throws Exception {
        mvc.perform(post("/api/cities").contentType("application/json").content(BODY.replace("120.123456", "120.1234567"))).andExpect(status().isBadRequest());
    }
    @Test void databaseDuplicateFallbackIs409() throws Exception {
        when(mapper.insert(any(City.class))).thenThrow(new org.springframework.dao.DuplicateKeyException("private SQL"));
        mvc.perform(post("/api/cities").contentType("application/json").content(BODY)).andExpect(status().isConflict());
    }
    @Test void unknownDatabaseErrorIsSafe500() throws Exception {
        when(mapper.insert(any(City.class))).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private connection details"));
        mvc.perform(post("/api/cities").contentType("application/json").content(BODY)).andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("internal server error"));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints={1062,1451,1452})
    void mysqlConstraintFallbackIsSafe409(int code) throws Exception {
        when(mapper.insert(any(City.class))).thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                "private SQL", new java.sql.SQLException("private detail", "23000", code)));
        mvc.perform(post("/api/cities").contentType("application/json").content(BODY)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("duplicate or referenced resource"));
    }
    @Test void rejectsLengthsAndCoordinateBounds() throws Exception {
        for (String body : new String[]{BODY.replace("TEMP_CITY","X".repeat(33)), BODY.replace("Test city","X".repeat(51)),
                BODY.replace("120.123456","180.000001"), BODY.replace("120.123456","-180.000001"),
                BODY.replace("36.5","90.000001"), BODY.replace("36.5","-90.000001")})
            mvc.perform(post("/api/cities").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        verify(mapper,never()).insert(any(City.class));
    }
}
