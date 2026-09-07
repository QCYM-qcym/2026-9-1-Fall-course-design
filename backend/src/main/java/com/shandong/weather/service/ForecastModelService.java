package com.shandong.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shandong.weather.entity.ForecastModel;
import com.shandong.weather.mapper.ForecastModelMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ForecastModelService {
    private final ForecastModelMapper mapper;

    public ForecastModelService(ForecastModelMapper mapper) {
        this.mapper = mapper;
    }

    public List<ForecastModel> listAll() {
        return mapper.selectList(new QueryWrapper<ForecastModel>().orderByAsc("id"));
    }
}
