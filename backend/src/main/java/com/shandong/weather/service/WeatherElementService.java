package com.shandong.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shandong.weather.entity.WeatherElement;
import com.shandong.weather.mapper.WeatherElementMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WeatherElementService {
    private final WeatherElementMapper mapper;

    public WeatherElementService(WeatherElementMapper mapper) {
        this.mapper = mapper;
    }

    public List<WeatherElement> listAll() {
        return mapper.selectList(new QueryWrapper<WeatherElement>().orderByAsc("id"));
    }
}
