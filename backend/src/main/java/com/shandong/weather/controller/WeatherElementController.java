package com.shandong.weather.controller;

import com.shandong.weather.common.ApiResponse;
import com.shandong.weather.entity.WeatherElement;
import com.shandong.weather.service.WeatherElementService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather-elements")
public class WeatherElementController {
    private final WeatherElementService service;

    public WeatherElementController(WeatherElementService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<WeatherElement>> listAll() {
        return ApiResponse.success(service.listAll());
    }
}
