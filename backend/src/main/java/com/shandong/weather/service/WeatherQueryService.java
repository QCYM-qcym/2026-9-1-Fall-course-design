package com.shandong.weather.service;

import com.shandong.weather.common.BusinessException;
import com.shandong.weather.mapper.ForecastRecordMapper;
import com.shandong.weather.mapper.WorkbenchRecordRow;
import com.shandong.weather.vo.WorkbenchVO;
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

    public WeatherQueryService(ForecastRecordMapper mapper,
            @Value("${weather.workbench.city-codes}") List<String> cityCodes,
            @Value("${weather.workbench.model-codes}") List<String> modelCodes,
            @Value("${weather.workbench.element-codes}") List<String> elementCodes) {
        this.mapper = mapper;
        this.cityCodes = List.copyOf(cityCodes);
        this.modelCodes = Set.copyOf(modelCodes);
        this.elementCodes = Set.copyOf(elementCodes);
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
