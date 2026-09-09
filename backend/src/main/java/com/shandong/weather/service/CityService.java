package com.shandong.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shandong.weather.common.BusinessException;
import com.shandong.weather.common.ManagementValidation;
import com.shandong.weather.dto.CityWriteRequest;
import com.shandong.weather.entity.City;
import com.shandong.weather.entity.ForecastRecord;
import com.shandong.weather.mapper.CityMapper;
import com.shandong.weather.mapper.ForecastRecordMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CityService {
    @Autowired
    private ForecastRecordMapper records;
    private final CityMapper mapper;

    public CityService(CityMapper mapper) {
        this.mapper = mapper;
    }

    public List<City> listAll() {
        return mapper.selectList(new QueryWrapper<City>().orderByAsc("id"));
    }

    @Transactional
    public City create(CityWriteRequest request) {
        unique(request.cityCode(), null);
        City city = from(request);
        mapper.insert(city);
        return city;
    }

    @Transactional
    public City update(Long id, CityWriteRequest request) {
        City old = require(id);
        if (!old.getCityCode().equals(request.cityCode()) && referenced(id))
            throw new BusinessException(409, "referenced city code cannot change");
        unique(request.cityCode(), id);
        City city = from(request); city.setId(id);
        if (mapper.updateById(city) != 1) throw new BusinessException(404, "city not found");
        return city;
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        if (referenced(id)) throw new BusinessException(409, "city is referenced");
        if (mapper.deleteById(id) != 1) throw new BusinessException(404, "city not found");
    }

    private boolean referenced(Long id) {
        return records.selectCount(new QueryWrapper<ForecastRecord>().eq("city_id", id)) > 0;
    }

    private void unique(String code, Long id) {
        var query = new QueryWrapper<City>().eq("city_code", code);
        if (id != null) query.ne("id", id);
        if (mapper.selectCount(query) > 0) throw new BusinessException(409, "city code already exists");
    }

    private City require(Long id) {
        ManagementValidation.id(id);
        City city = mapper.selectOne(new QueryWrapper<City>().eq("id", id).last("FOR UPDATE"));
        if (city == null) throw new BusinessException(404, "city not found");
        return city;
    }

    private City from(CityWriteRequest request) {
        City city = new City();
        city.setCityCode(request.cityCode()); city.setCityName(request.cityName().trim());
        city.setLongitude(request.longitude()); city.setLatitude(request.latitude());
        return city;
    }
}
