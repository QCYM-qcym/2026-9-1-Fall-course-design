package com.shandong.weather.controller;

import com.shandong.weather.common.ApiResponse;
import com.shandong.weather.entity.City;
import com.shandong.weather.service.CityService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cities")
public class CityController {
    private final CityService service;

    public CityController(CityService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<City>> listAll() {
        return ApiResponse.success(service.listAll());
    }
}
