package com.shandong.weather.vo;
import java.math.BigDecimal;
public record ForecastRecordVO(Long id, Long cityId, String cityCode, String cityName,
    Long modelId, String modelCode, String modelName, Long elementId,
    String elementCode, String elementName, String unit, String forecastTime, BigDecimal value) {}
