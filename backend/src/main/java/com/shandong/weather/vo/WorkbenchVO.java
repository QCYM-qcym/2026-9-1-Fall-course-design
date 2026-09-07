package com.shandong.weather.vo;

import java.math.BigDecimal;
import java.util.List;

public record WorkbenchVO(Model model, Element element, List<String> times, List<RecordValue> records) {
    public record Model(long id, String modelCode, String modelName) {
    }

    public record Element(long id, String elementCode, String elementName, String unit) {
    }

    public record RecordValue(long cityId, String cityName, String forecastTime, BigDecimal value) {
    }
}
