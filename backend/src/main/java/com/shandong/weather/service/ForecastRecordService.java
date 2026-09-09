package com.shandong.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shandong.weather.common.BusinessException;
import com.shandong.weather.common.ManagementValidation;
import com.shandong.weather.dto.ForecastRecordWriteRequest;
import com.shandong.weather.entity.*;
import com.shandong.weather.mapper.*;
import com.shandong.weather.vo.ForecastRecordVO;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForecastRecordService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ForecastRecordMapper records;
    private final CityMapper cities;
    private final ForecastModelMapper models;
    private final WeatherElementMapper elements;
    public ForecastRecordService(ForecastRecordMapper records, CityMapper cities, ForecastModelMapper models, WeatherElementMapper elements) {
        this.records=records; this.cities=cities; this.models=models; this.elements=elements;
    }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<ForecastRecordVO> listAll() {
        var rows = records.selectList(new QueryWrapper<ForecastRecord>().orderByAsc("id"));
        if (rows.isEmpty()) return List.of();
        var cityMap = cities.selectBatchIds(rows.stream().map(ForecastRecord::getCityId).distinct().toList()).stream().collect(Collectors.toMap(City::getId, c -> c));
        var modelMap = models.selectBatchIds(rows.stream().map(ForecastRecord::getModelId).distinct().toList()).stream().collect(Collectors.toMap(ForecastModel::getId, m -> m));
        var elementMap = elements.selectBatchIds(rows.stream().map(ForecastRecord::getElementId).distinct().toList()).stream().collect(Collectors.toMap(WeatherElement::getId, e -> e));
        return rows.stream().map(r -> vo(r, cityMap.get(r.getCityId()), modelMap.get(r.getModelId()), elementMap.get(r.getElementId()))).toList();
    }
    @Transactional
    public ForecastRecordVO create(ForecastRecordWriteRequest request) {
        // Lock dictionary rows in city -> model -> element order before checking semantics.
        var city = cities.selectOne(new QueryWrapper<City>().eq("id", request.cityId()).last("FOR UPDATE"));
        var model = models.selectOne(new QueryWrapper<ForecastModel>().eq("id", request.modelId()).last("FOR UPDATE"));
        var element = elements.selectOne(new QueryWrapper<WeatherElement>().eq("id", request.elementId()).last("FOR UPDATE"));
        associations(city, model, element);
        value(request, element);
        unique(request, null);
        var row = from(request); records.insert(row);
        return vo(row, city, model, element);
    }
    @Transactional
    public ForecastRecordVO update(Long id, ForecastRecordWriteRequest request) {
        require(id);
        var city = cities.selectOne(new QueryWrapper<City>().eq("id", request.cityId()).last("FOR UPDATE"));
        var model = models.selectOne(new QueryWrapper<ForecastModel>().eq("id", request.modelId()).last("FOR UPDATE"));
        var element = elements.selectOne(new QueryWrapper<WeatherElement>().eq("id", request.elementId()).last("FOR UPDATE"));
        associations(city, model, element);
        value(request, element);
        unique(request, id);
        var row = from(request); row.setId(id);
        if (records.updateById(row) != 1) throw new BusinessException(404, "record not found");
        return vo(row, city, model, element);
    }
    @Transactional
    public void delete(Long id) {
        require(id);
        if (records.deleteById(id) != 1) throw new BusinessException(404, "record not found");
    }
    private void require(Long id) {
        ManagementValidation.id(id);
        if (records.selectById(id) == null) throw new BusinessException(404, "record not found");
    }
    private void associations(City city, ForecastModel model, WeatherElement element) {
        if (city == null || model == null || element == null) throw new BusinessException(404, "associated dictionary not found");
    }
    private void value(ForecastRecordWriteRequest request, WeatherElement element) {
        if ("PRECIP".equals(element.getElementCode()) && request.value().signum() < 0)
            throw new BusinessException(400, "precipitation cannot be negative");
    }
    private void unique(ForecastRecordWriteRequest request, Long id) {
        var q = new QueryWrapper<ForecastRecord>().eq("city_id",request.cityId()).eq("model_id",request.modelId())
            .eq("element_id",request.elementId()).eq("forecast_time",request.forecastTime());
        if (id != null) q.ne("id",id);
        if (records.selectCount(q) > 0) throw new BusinessException(409,"forecast record already exists");
    }
    private ForecastRecord from(ForecastRecordWriteRequest request) {
        var row = new ForecastRecord(); row.setCityId(request.cityId()); row.setModelId(request.modelId());
        row.setElementId(request.elementId()); row.setForecastTime(request.forecastTime()); row.setValue(request.value());
        return row;
    }
    private ForecastRecordVO vo(ForecastRecord r, City c, ForecastModel m, WeatherElement e) {
        if (c == null || m == null || e == null) throw new BusinessException(500,"inconsistent dictionary data");
        return new ForecastRecordVO(r.getId(),c.getId(),c.getCityCode(),c.getCityName(),m.getId(),m.getModelCode(),m.getModelName(),
            e.getId(),e.getElementCode(),e.getElementName(),e.getUnit(),TIME.format(r.getForecastTime()),r.getValue());
    }
}
