package com.shandong.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shandong.weather.common.BusinessException;
import com.shandong.weather.common.ManagementValidation;
import com.shandong.weather.dto.WeatherElementWriteRequest;
import com.shandong.weather.entity.ForecastRecord;
import com.shandong.weather.entity.WeatherElement;
import com.shandong.weather.mapper.ForecastRecordMapper;
import com.shandong.weather.mapper.WeatherElementMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeatherElementService {
    @Autowired
    private ForecastRecordMapper records;
    private final WeatherElementMapper mapper;

    public WeatherElementService(WeatherElementMapper mapper) {
        this.mapper = mapper;
    }

    public List<WeatherElement> listAll() {
        return mapper.selectList(new QueryWrapper<WeatherElement>().orderByAsc("id"));
    }

    @Transactional
    public WeatherElement create(WeatherElementWriteRequest request) {
        validateUnit(request);
        unique(request.elementCode(), null);
        WeatherElement element = from(request);
        mapper.insert(element);
        return element;
    }

    @Transactional
    public WeatherElement update(Long id, WeatherElementWriteRequest request) {
        WeatherElement old = require(id);
        if ((!old.getElementCode().equals(request.elementCode()) || !old.getUnit().equals(request.unit().trim())) && referenced(id))
            throw new BusinessException(409, "referenced element code cannot change");
        validateUnit(request);
        unique(request.elementCode(), id);
        WeatherElement element = from(request); element.setId(id);
        if (mapper.updateById(element) != 1) throw new BusinessException(404, "element not found");
        return element;
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        if (referenced(id)) throw new BusinessException(409, "element is referenced");
        if (mapper.deleteById(id) != 1) throw new BusinessException(404, "element not found");
    }

    private boolean referenced(Long id) {
        return records.selectCount(new QueryWrapper<ForecastRecord>().eq("element_id", id)) > 0;
    }

    private void unique(String code, Long id) {
        var query = new QueryWrapper<WeatherElement>().eq("element_code", code);
        if (id != null) query.ne("id", id);
        if (mapper.selectCount(query) > 0) throw new BusinessException(409, "element code already exists");
    }

    private WeatherElement require(Long id) {
        ManagementValidation.id(id);
        WeatherElement element = mapper.selectOne(new QueryWrapper<WeatherElement>().eq("id", id).last("FOR UPDATE"));
        if (element == null) throw new BusinessException(404, "element not found");
        return element;
    }

    private void validateUnit(WeatherElementWriteRequest request) {
        String unit = request.unit().trim();
        if (("T2M".equals(request.elementCode()) && !"℃".equals(unit)) ||
                ("PRECIP".equals(request.elementCode()) && !"mm".equals(unit)))
            throw new BusinessException(400, "invalid core element unit");
    }

    private WeatherElement from(WeatherElementWriteRequest request) {
        WeatherElement element = new WeatherElement();
        element.setElementCode(request.elementCode()); element.setElementName(request.elementName().trim());
        element.setUnit(request.unit().trim());
        return element;
    }
}
