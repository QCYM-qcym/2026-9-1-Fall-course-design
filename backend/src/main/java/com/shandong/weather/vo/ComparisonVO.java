package com.shandong.weather.vo;

import java.math.BigDecimal;
import java.util.List;

public record ComparisonVO(long cityId, String cityName, ElementInfo element, List<ModelSeries> series) {
    public record ElementInfo(long id, String elementCode, String elementName, String unit) {
    }

    public record ModelSeries(long modelId, String modelCode, String modelName, List<SeriesPoint> values) {
    }

    public record SeriesPoint(String forecastTime, BigDecimal value) {
    }
}
