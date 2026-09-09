package com.shandong.weather.common;
public final class ManagementValidation {
    private ManagementValidation() {}
    public static void id(Long id) {
        if (id == null || id < 1 || id > 9007199254740991L)
            throw new BusinessException(400, "id must be a positive safe integer");
    }
}
