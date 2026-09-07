package com.shandong.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shandong.weather.entity.City;
import com.shandong.weather.mapper.CityMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CityService {
    private final CityMapper mapper;

    public CityService(CityMapper mapper) {
        this.mapper = mapper;
    }

    public List<City> listAll() {
        return mapper.selectList(new QueryWrapper<City>().orderByAsc("id"));
    }
}
