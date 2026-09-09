package com.shandong.weather.dto;
import java.math.BigDecimal;
import javax.validation.constraints.*;
public record CityWriteRequest(
    @NotBlank @Size(max=32) @Pattern(regexp="[A-Z0-9_]+") String cityCode,
    @NotBlank @Size(max=50) String cityName,
    @NotNull @DecimalMin("-180") @DecimalMax("180") @Digits(integer=4,fraction=6) BigDecimal longitude,
    @NotNull @DecimalMin("-90") @DecimalMax("90") @Digits(integer=4,fraction=6) BigDecimal latitude) {}
