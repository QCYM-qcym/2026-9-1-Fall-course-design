package com.shandong.weather.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
public record ForecastRecordWriteRequest(
    @NotNull @Min(1) @Max(9007199254740991L) @JsonDeserialize(using=StrictIdDeserializer.class) Long cityId,
    @NotNull @Min(1) @Max(9007199254740991L) @JsonDeserialize(using=StrictIdDeserializer.class) Long modelId,
    @NotNull @Min(1) @Max(9007199254740991L) @JsonDeserialize(using=StrictIdDeserializer.class) Long elementId,
    @NotNull @JsonDeserialize(using=LocalBusinessTimeDeserializer.class) LocalDateTime forecastTime,
    @NotNull @Digits(integer=8,fraction=2) BigDecimal value) {}
