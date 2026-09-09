package com.shandong.weather.controller;
import com.shandong.weather.common.ApiResponse;
import com.shandong.weather.dto.ForecastRecordWriteRequest;
import com.shandong.weather.service.ForecastRecordService;
import com.shandong.weather.vo.ForecastRecordVO;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forecast-records")
public class ForecastRecordController {
    private final ForecastRecordService service;
    public ForecastRecordController(ForecastRecordService service) { this.service = service; }
    @GetMapping
    public ApiResponse<List<ForecastRecordVO>> listAll() { return ApiResponse.success(service.listAll()); }
    @PostMapping
    public ApiResponse<ForecastRecordVO> create(@Valid @RequestBody ForecastRecordWriteRequest request) {
        return ApiResponse.success(service.create(request));
    }
    @PutMapping("/{id}")
    public ApiResponse<ForecastRecordVO> update(@PathVariable("id") Long id, @Valid @RequestBody ForecastRecordWriteRequest request) {
        return ApiResponse.success(service.update(id,request));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) { service.delete(id); return ApiResponse.success(null); }
}
