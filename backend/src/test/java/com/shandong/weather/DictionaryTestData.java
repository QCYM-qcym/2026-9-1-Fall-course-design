package com.shandong.weather;

import com.shandong.weather.entity.City;
import com.shandong.weather.entity.ForecastModel;
import com.shandong.weather.entity.WeatherElement;
import java.math.BigDecimal;
import java.util.List;

// Fixtures include non-core codes and non-seed IDs to detect hardcoded dictionaries or filtering.
final class DictionaryTestData {
    static List<City> cities() {
        var first = new City();
        first.setId(8L);
        first.setCityCode("JINAN");
        first.setCityName("济南");
        first.setLongitude(new BigDecimal("116.997222"));
        first.setLatitude(new BigDecimal("36.668333"));
        var second = new City();
        second.setId(40L);
        second.setCityCode("TEMP_CITY");
        second.setCityName("临时城市");
        second.setLongitude(new BigDecimal("120.380420"));
        second.setLatitude(new BigDecimal("36.064880"));
        return List.of(first, second);
    }

    static List<ForecastModel> models() {
        var first = new ForecastModel();
        first.setId(41L);
        first.setModelCode("ECMWF");
        first.setModelName("数据库模型名称");
        first.setDescription("数据库模型说明");
        var second = new ForecastModel();
        second.setId(90L);
        second.setModelCode("TEMP_MODEL");
        second.setModelName("临时模型");
        second.setDescription(null);
        return List.of(first, second);
    }

    static List<WeatherElement> elements() {
        var first = new WeatherElement();
        first.setId(71L);
        first.setElementCode("T2M");
        first.setElementName("2 米气温");
        first.setUnit("℃");
        var second = new WeatherElement();
        second.setId(80L);
        second.setElementCode("TEMP_ELEMENT");
        second.setElementName("临时要素");
        second.setUnit("mm");
        return List.of(first, second);
    }

    private DictionaryTestData() {
    }
}
