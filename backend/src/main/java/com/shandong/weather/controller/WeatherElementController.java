package com.shandong.weather.controller;

import com.shandong.weather.common.ApiResponse;
import com.shandong.weather.dto.WeatherElementWriteRequest;
import com.shandong.weather.entity.WeatherElement;
import com.shandong.weather.service.WeatherElementService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather-elements")
public class WeatherElementController {
    @PostMapping
    public ApiResponse<WeatherElement> create(@Valid @RequestBody WeatherElementWriteRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<WeatherElement> update(@PathVariable("id") Long id,
            @Valid @RequestBody WeatherElementWriteRequest request) {
        return ApiResponse.success(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        service.delete(id); return ApiResponse.success(null);
    }
    private final WeatherElementService service;

    public WeatherElementController(WeatherElementService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<WeatherElement>> listAll() {
        return ApiResponse.success(service.listAll());
    }
}
