package com.shandong.weather.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// SQL projection; public series and statistics are assembled by the Service.
public record TrendRecordRow(String elementCode, LocalDateTime forecastTime, BigDecimal value) {
}
