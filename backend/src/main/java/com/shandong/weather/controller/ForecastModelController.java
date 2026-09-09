package com.shandong.weather.controller;

import com.shandong.weather.common.ApiResponse;
import com.shandong.weather.dto.ForecastModelWriteRequest;
import com.shandong.weather.entity.ForecastModel;
import com.shandong.weather.service.ForecastModelService;
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
@RequestMapping("/api/forecast-models")
public class ForecastModelController {
    @PostMapping
    public ApiResponse<ForecastModel> create(@Valid @RequestBody ForecastModelWriteRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ForecastModel> update(@PathVariable("id") Long id,
            @Valid @RequestBody ForecastModelWriteRequest request) {
        return ApiResponse.success(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        service.delete(id); return ApiResponse.success(null);
    }
    private final ForecastModelService service;

    public ForecastModelController(ForecastModelService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ForecastModel>> listAll() {
        return ApiResponse.success(service.listAll());
    }
}
