package com.shandong.weather.service;

import com.shandong.weather.common.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shandong.weather.entity.WeatherElement;
import com.shandong.weather.mapper.CityMapper;
import com.shandong.weather.mapper.ForecastModelMapper;
import com.shandong.weather.mapper.WeatherElementMapper;
import com.shandong.weather.mapper.TrendRecordRow;
import com.shandong.weather.mapper.ForecastRecordMapper;
import com.shandong.weather.mapper.WorkbenchRecordRow;
import com.shandong.weather.vo.WorkbenchVO;
import com.shandong.weather.vo.TrendVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WeatherQueryService {
    private static final long MAX_SAFE_ID = 9007199254740991L;
    private static final DateTimeFormatter RESPONSE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    private final ForecastRecordMapper mapper;
    private final List<String> cityCodes;
    private final Set<String> modelCodes;
    private final Set<String> elementCodes;
    private final CityMapper cities;
    private final ForecastModelMapper models;
    private final WeatherElementMapper elements;

    public WeatherQueryService(ForecastRecordMapper mapper,
            @Value("${weather.workbench.city-codes}") List<String> cityCodes,
            @Value("${weather.workbench.model-codes}") List<String> modelCodes,
            @Value("${weather.workbench.element-codes}") List<String> elementCodes,
            CityMapper cities, ForecastModelMapper models, WeatherElementMapper elements) {
        this.mapper = mapper;
        this.cityCodes = List.copyOf(cityCodes);
        this.modelCodes = Set.copyOf(modelCodes);
        this.elementCodes = Set.copyOf(elementCodes);
        this.cities = cities;
        this.models = models;
        this.elements = elements;
    }

    public TrendVO trend(Long cityId, Long modelId, LocalDateTime startTime, LocalDateTime endTime) {
        validateId(cityId, "cityId");
        validateId(modelId, "modelId");
        if (startTime == null || endTime == null || startTime.isAfter(endTime)) {
            throw new BusinessException(400, "startTime and endTime are required; startTime must not exceed endTime");
        }
        var city = cities.selectById(cityId);
        if (city == null) {
            throw new BusinessException(404, "cityId does not exist");
        }
        var model = models.selectById(modelId);
        if (model == null) {
            throw new BusinessException(404, "modelId does not exist");
        }
        // Existence checks precede the supported-display restriction, as in Workbench.
        if (!cityCodes.contains(city.getCityCode()) || !modelCodes.contains(model.getModelCode())) {
            throw new BusinessException(400, "city or model is not supported by trend");
        }
        var coreElements = elements.selectList(new QueryWrapper<WeatherElement>()
                .in("element_code", List.of("T2M", "PRECIP")));
        var temperatureElement = requireCoreElement(coreElements, "T2M", "℃");
        var precipitationElement = requireCoreElement(coreElements, "PRECIP", "mm");
        var rows = mapper.selectTrendData(cityId, modelId,
                List.of(temperatureElement.getId(), precipitationElement.getId()), startTime, endTime);
        var temperature = trendSeries(rows, "T2M");
        var precipitation = trendSeries(rows, "PRECIP");
        var max = temperature.stream().map(TrendVO.SeriesPoint::value).max(BigDecimal::compareTo).orElse(null);
        var min = temperature.stream().map(TrendVO.SeriesPoint::value).min(BigDecimal::compareTo).orElse(null);
        var average = temperature.isEmpty() ? null : sumSeries(temperature)
                .divide(BigDecimal.valueOf(temperature.size()), 2, RoundingMode.HALF_UP);
        // Empty is unknown, whereas observed all-zero precipitation is a real zero.
        var total = precipitation.isEmpty() ? null : sumSeries(precipitation);
        return new TrendVO(city.getId(), city.getCityName(), model.getId(), model.getModelName(),
                temperature, precipitation, new TrendVO.Statistics(max, min, average, total));
    }

    private WeatherElement requireCoreElement(List<WeatherElement> entries, String code, String unit) {
        var element = entries.stream().filter(entry -> code.equals(entry.getElementCode())).findFirst()
                .orElseThrow(() -> new BusinessException(500, "core weather element configuration is missing"));
        if (!unit.equals(element.getUnit())) {
            throw new BusinessException(500, "core weather element unit configuration is invalid");
        }
        return element;
    }

    private List<TrendVO.SeriesPoint> trendSeries(List<TrendRecordRow> rows, String code) {
        return rows.stream().filter(row -> code.equals(row.elementCode()))
                .sorted(Comparator.comparing(TrendRecordRow::forecastTime))
                .map(row -> new TrendVO.SeriesPoint(RESPONSE_TIME.format(row.forecastTime()), row.value())).toList();
    }

    private BigDecimal sumSeries(List<TrendVO.SeriesPoint> series) {
        return series.stream().map(TrendVO.SeriesPoint::value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public WorkbenchVO workbench(Long modelId, Long elementId, LocalDateTime startTime, LocalDateTime endTime) {
        validateId(modelId, "modelId");
        validateId(elementId, "elementId");
        if (startTime == null || endTime == null || startTime.isAfter(endTime)) {
            throw new BusinessException(400, "startTime and endTime are required; startTime must not exceed endTime");
        }

        // Check both associations before applying the supported-dictionary restriction.
        var model = mapper.selectWorkbenchModel(modelId);
        if (model == null) {
            throw new BusinessException(404, "modelId does not exist");
        }
        var element = mapper.selectWorkbenchElement(elementId);
        if (element == null) {
            throw new BusinessException(404, "elementId does not exist");
        }
        if (!modelCodes.contains(model.modelCode()) || !elementCodes.contains(element.elementCode())) {
            throw new BusinessException(400, "model or element is not supported by the workbench");
        }

        var rows = mapper.selectWorkbenchData(modelId, elementId, startTime, endTime, cityCodes);
        var times = rows.stream().map(WorkbenchRecordRow::forecastTime)
                .distinct().sorted().map(RESPONSE_TIME::format).toList();
        var records = rows.stream().map(row -> new WorkbenchVO.RecordValue(
                row.cityId(), row.cityName(), RESPONSE_TIME.format(row.forecastTime()), row.value())).toList();
        return new WorkbenchVO(
                new WorkbenchVO.Model(model.id(), model.modelCode(), model.modelName()),
                new WorkbenchVO.Element(element.id(), element.elementCode(), element.elementName(), element.unit()),
                times, records);
    }

    private void validateId(Long id, String name) {
        if (id == null || id < 1 || id > MAX_SAFE_ID) {
            throw new BusinessException(400, name + " must be an integer between 1 and " + MAX_SAFE_ID);
        }
    }
}
