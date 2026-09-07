package com.shandong.weather.controller;

import com.shandong.weather.common.ApiResponse;
import com.shandong.weather.entity.ForecastModel;
import com.shandong.weather.service.ForecastModelService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast-models")
public class ForecastModelController {
    private final ForecastModelService service;

    public ForecastModelController(ForecastModelService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ForecastModel>> listAll() {
        return ApiResponse.success(service.listAll());
    }
}
