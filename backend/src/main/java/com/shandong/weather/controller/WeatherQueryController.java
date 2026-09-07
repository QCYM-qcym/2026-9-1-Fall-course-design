package com.shandong.weather.controller;

import com.shandong.weather.common.ApiResponse;
import com.shandong.weather.common.BusinessException;
import com.shandong.weather.service.WeatherQueryService;
import com.shandong.weather.vo.WorkbenchVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
public class WeatherQueryController {
    private static final Pattern TIME_SHAPE = Pattern.compile(
            "[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}");
    // uuuu + STRICT rejects impossible dates instead of silently normalizing them.
    private static final DateTimeFormatter REQUEST_TIME = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    private final WeatherQueryService service;

    public WeatherQueryController(WeatherQueryService service) {
        this.service = service;
    }

    @GetMapping("/workbench")
    public ApiResponse<WorkbenchVO> workbench(
            @RequestParam("modelId") Long modelId,
            @RequestParam("elementId") Long elementId,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime) {
        return ApiResponse.success(service.workbench(
                modelId, elementId, parseTime(startTime), parseTime(endTime)));
    }

    private LocalDateTime parseTime(String value) {
        if (value == null || !TIME_SHAPE.matcher(value).matches()) {
            throw new BusinessException(400, "time must use yyyy-MM-dd HH:mm:ss");
        }
        try {
            return LocalDateTime.parse(value, REQUEST_TIME);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(400, "time must be a valid local date in yyyy-MM-dd HH:mm:ss");
        }
    }
}
