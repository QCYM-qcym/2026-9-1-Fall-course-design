package com.shandong.weather.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shandong.weather.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("SELECT id, username, password_hash, role, enabled FROM sys_user WHERE username = #{username}")
    SysUser findByUsername(String username);
}
