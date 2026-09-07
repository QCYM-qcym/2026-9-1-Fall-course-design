package com.shandong.weather;

import com.shandong.weather.mapper.WorkbenchElementRow;
import com.shandong.weather.mapper.WorkbenchModelRow;
import com.shandong.weather.mapper.WorkbenchRecordRow;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

final class WorkbenchTestData {
    static final LocalDateTime START = LocalDateTime.of(2026, 9, 7, 8, 0);
    static final LocalDateTime END = LocalDateTime.of(2026, 9, 7, 14, 0);
    static final List<String> CITIES = List.of("JINAN", "QINGDAO");
    static final WorkbenchModelRow MODEL = new WorkbenchModelRow(41L, "ECMWF", "Database model name");
    static final WorkbenchElementRow ELEMENT = new WorkbenchElementRow(71L, "T2M", "2 米气温", "℃");

    static WorkbenchRecordRow row(long cityId, String cityName, LocalDateTime time, String value) {
        return new WorkbenchRecordRow(cityId, cityName, time, new BigDecimal(value));
    }

    private WorkbenchTestData() {
    }
}
