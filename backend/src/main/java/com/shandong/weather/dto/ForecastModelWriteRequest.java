package com.shandong.weather.dto;
import javax.validation.constraints.*;
public record ForecastModelWriteRequest(
    @NotBlank @Size(max=32) @Pattern(regexp="[A-Z0-9_]+") String modelCode,
    @NotBlank @Size(max=50) String modelName,
    @Size(max=255) String description) {}
