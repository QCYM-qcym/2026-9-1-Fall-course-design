package com.shandong.weather.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Internal SQL projection; the Service supplies validated dictionary metadata.
public record ComparisonRecordRow(String modelCode, LocalDateTime forecastTime, BigDecimal value) {
}
