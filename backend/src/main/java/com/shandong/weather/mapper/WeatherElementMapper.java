package com.shandong.weather.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shandong.weather.entity.WeatherElement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WeatherElementMapper extends BaseMapper<WeatherElement> {
}
