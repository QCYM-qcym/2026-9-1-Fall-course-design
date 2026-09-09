package com.shandong.weather.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@TableName("forecast_record")
public class ForecastRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long cityId;
    private Long modelId;
    private Long elementId;
    private LocalDateTime forecastTime;
    private BigDecimal value;
    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public Long getCityId() { return cityId; }
    public void setCityId(Long value) { this.cityId = value; }
    public Long getModelId() { return modelId; }
    public void setModelId(Long value) { this.modelId = value; }
    public Long getElementId() { return elementId; }
    public void setElementId(Long value) { this.elementId = value; }
    public LocalDateTime getForecastTime() { return forecastTime; }
    public void setForecastTime(LocalDateTime value) { this.forecastTime = value; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
}
