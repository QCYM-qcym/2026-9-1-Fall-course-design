package com.shandong.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real HTTP -> Service -> MyBatis -> MySQL; every test rolls back all row changes. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class ManagementCrudIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @BeforeTransaction
    @AfterTransaction
    void baselineOutsideTransaction() {
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("shandong_weather");
        for (var entry : java.util.Map.of("city",16L,"forecast_model",2L,"weather_element",2L,"forecast_record",192L).entrySet())
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + entry.getKey(), Long.class)).isEqualTo(entry.getValue());
    }

    private JsonNode create(String path, String body) throws Exception {
        return json.readTree(mvc.perform(post(path).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString()).get("data");
    }
    private void update(String path, long id, String body) throws Exception {
        mvc.perform(put(path + "/" + id).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(id));
    }
    private void remove(String path, long id) throws Exception {
        mvc.perform(delete(path + "/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
        mvc.perform(delete(path + "/" + id)).andExpect(status().isNotFound());
    }
    private String suffix() { return UUID.randomUUID().toString().replace("-","").substring(0,16).toUpperCase(); }

    @Test void fourResourceLifecycleAndReferenceProtectionRollBack() throws Exception {
        String key = suffix();
        String cityBody = "{\"cityCode\":\"C_" + key + "\",\"cityName\":\"Temporary city\",\"longitude\":120.123456,\"latitude\":36.123456}";
        String modelBody = "{\"modelCode\":\"M_" + key + "\",\"modelName\":\"Temporary model\",\"description\":\"description\"}";
        String elementBody = "{\"elementCode\":\"E_" + key + "\",\"elementName\":\"Temporary element\",\"unit\":\"mm\"}";
        long city = create("/api/cities",cityBody).get("id").asLong();
        long model = create("/api/forecast-models",modelBody).get("id").asLong();
        long element = create("/api/weather-elements",elementBody).get("id").asLong();
        String recordBody = "{\"cityId\":" + city + ",\"modelId\":" + model + ",\"elementId\":" + element + ",\"forecastTime\":\"2026-09-07 08:00:00\",\"value\":0.00}";
        JsonNode row = create("/api/forecast-records",recordBody);
        long record = row.get("id").asLong();
        assertThat(row.get("cityCode").asText()).isEqualTo("C_" + key);
        assertThat(row.get("value").decimalValue()).isEqualByComparingTo("0.00");
        mvc.perform(post("/api/forecast-records").contentType("application/json").content(recordBody)).andExpect(status().isConflict());
        for (var entry : java.util.Map.of("/api/cities",city,"/api/forecast-models",model,"/api/weather-elements",element).entrySet())
            mvc.perform(delete(entry.getKey() + "/" + entry.getValue())).andExpect(status().isConflict());
        mvc.perform(put("/api/cities/" + city).contentType("application/json").content(cityBody.replace("C_" + key,"X_" + key))).andExpect(status().isConflict());
        mvc.perform(put("/api/forecast-models/" + model).contentType("application/json").content(modelBody.replace("M_" + key,"X_" + key))).andExpect(status().isConflict());
        mvc.perform(put("/api/weather-elements/" + element).contentType("application/json").content(elementBody.replace("E_" + key,"X_" + key))).andExpect(status().isConflict());
        mvc.perform(put("/api/weather-elements/" + element).contentType("application/json").content(elementBody.replace("mm","kg"))).andExpect(status().isConflict());
        update("/api/cities",city,cityBody.replace("Temporary city","Renamed city"));
        update("/api/forecast-models",model,modelBody.replace("\"description\":\"description\"","\"description\":null"));
        assertThat(jdbc.queryForObject("SELECT description FROM forecast_model WHERE id=?",String.class,model)).isNull();
        update("/api/weather-elements",element,elementBody.replace("Temporary element","Renamed element"));
        update("/api/forecast-records",record,recordBody.replace("0.00","1.25"));
        assertThat(jdbc.queryForObject("SELECT value FROM forecast_record WHERE id=?",java.math.BigDecimal.class,record)).isEqualByComparingTo("1.25");
        JsonNode list = json.readTree(mvc.perform(get("/api/forecast-records")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("data");
        assertThat(list.size()).isEqualTo(193);
        long previous = 0;
        for (JsonNode item : list) { assertThat(item.get("id").asLong()).isGreaterThan(previous); previous=item.get("id").asLong(); }
        remove("/api/forecast-records",record);
        remove("/api/cities",city);
        remove("/api/forecast-models",model);
        remove("/api/weather-elements",element);
    }

    @Test void insertedRowsAreLeftForRollbackNotManualCleanup() throws Exception {
        String body = "{\"cityCode\":\"C_" + suffix() + "\",\"cityName\":\"Rollback proof\",\"longitude\":180,\"latitude\":-90}";
        create("/api/cities",body);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM city",Long.class)).isEqualTo(17);
        // @AfterTransaction must observe 16 again, including when assertions fail.
    }

    @Test void realCoreQueriesRemainCompatible() throws Exception {
        for (String path : new String[]{"/api/cities","/api/forecast-models","/api/weather-elements"})
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        long city = jdbc.queryForObject("SELECT id FROM city WHERE city_code='JINAN'",Long.class);
        long model = jdbc.queryForObject("SELECT id FROM forecast_model WHERE model_code='ECMWF'",Long.class);
        long element = jdbc.queryForObject("SELECT id FROM weather_element WHERE element_code='T2M'",Long.class);
        for (String path : new String[]{"/api/weather/workbench","/api/weather/trend","/api/weather/comparison"})
            mvc.perform(get(path).param("cityId",Long.toString(city)).param("modelId",Long.toString(model))
                .param("elementId",Long.toString(element)).param("startTime","2026-09-07 08:00:00").param("endTime","2026-09-07 14:00:00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}
