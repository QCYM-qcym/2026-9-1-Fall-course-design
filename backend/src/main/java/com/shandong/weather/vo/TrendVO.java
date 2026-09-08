package com.shandong.weather.vo;

import java.math.BigDecimal;
import java.util.List;

public record TrendVO(long cityId, String cityName, long modelId, String modelName,
        List<SeriesPoint> temperature, List<SeriesPoint> precipitation, Statistics statistics) {
    public record SeriesPoint(String forecastTime, BigDecimal value) {
    }

    public record Statistics(BigDecimal temperatureMax, BigDecimal temperatureMin,
            BigDecimal temperatureAvg, BigDecimal precipitationTotal) {
    }
}
