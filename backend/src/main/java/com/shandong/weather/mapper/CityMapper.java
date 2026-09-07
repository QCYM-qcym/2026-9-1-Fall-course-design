package com.shandong.weather.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shandong.weather.entity.City;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CityMapper extends BaseMapper<City> {
}
