package com.shandong.weather.dto;
import javax.validation.constraints.*;
public record WeatherElementWriteRequest(
    @NotBlank @Size(max=32) @Pattern(regexp="[A-Z0-9_]+") String elementCode,
    @NotBlank @Size(max=50) String elementName,
    @NotBlank @Size(max=16) String unit) {}
