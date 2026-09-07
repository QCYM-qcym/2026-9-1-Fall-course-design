package com.shandong.weather.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Internal SQL projection, deliberately separate from the public Workbench response.
public record WorkbenchRecordRow(long cityId, String cityName, LocalDateTime forecastTime, BigDecimal value) {
}
