package com.shandong.weather.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shandong.weather.entity.ForecastModel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ForecastModelMapper extends BaseMapper<ForecastModel> {
}
