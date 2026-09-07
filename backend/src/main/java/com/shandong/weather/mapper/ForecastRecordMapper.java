package com.shandong.weather.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ForecastRecordMapper {
    WorkbenchModelRow selectWorkbenchModel(@Param("modelId") long modelId);

    WorkbenchElementRow selectWorkbenchElement(@Param("elementId") long elementId);

    List<WorkbenchRecordRow> selectWorkbenchData(
            @Param("modelId") long modelId,
            @Param("elementId") long elementId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("cityCodes") List<String> cityCodes);
}
