package com.shandong.weather.controller;

import com.shandong.weather.common.ApiResponse;
import com.shandong.weather.dto.CityWriteRequest;
import com.shandong.weather.entity.City;
import com.shandong.weather.service.CityService;
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
@RequestMapping("/api/cities")
public class CityController {
    @PostMapping
    public ApiResponse<City> create(@Valid @RequestBody CityWriteRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<City> update(@PathVariable("id") Long id,
            @Valid @RequestBody CityWriteRequest request) {
        return ApiResponse.success(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        service.delete(id); return ApiResponse.success(null);
    }
    private final CityService service;

    public CityController(CityService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<City>> listAll() {
        return ApiResponse.success(service.listAll());
    }
}
