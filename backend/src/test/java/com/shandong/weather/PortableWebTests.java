package com.shandong.weather;

import com.shandong.weather.controller.HealthController;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@BusinessSecurityTest
@org.springframework.security.test.context.support.WithMockUser(roles = "USER")
class PortableWebTests {
    @Autowired MockMvc mvc;
    @org.springframework.boot.test.mock.mockito.MockBean com.shandong.weather.service.CityService cities;
    @org.springframework.boot.test.mock.mockito.MockBean com.shandong.weather.service.ForecastModelService models;
    @org.springframework.boot.test.mock.mockito.MockBean com.shandong.weather.service.WeatherElementService elements;
    @org.springframework.boot.test.mock.mockito.MockBean com.shandong.weather.service.ForecastRecordService records;
    @org.springframework.boot.test.mock.mockito.MockBean com.shandong.weather.service.WeatherQueryService weather;

    @ParameterizedTest
    @ValueSource(strings = {"/login", "/weather", "/analysis", "/comparison", "/management"})
    void onlyKnownPageRoutesForwardToBundledIndex(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/not-found", "/assets/missing.js", "/missing.css", "/weather/missing", "/unknown"})
    void missingApiAndAssetsNeverBecomeTheSpa(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().is(path.startsWith("/assets/") ? 404 : 403)).andExpect(forwardedUrl(null));
    }
}
