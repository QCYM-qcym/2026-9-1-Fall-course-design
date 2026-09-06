-- Shandong weather visualization database schema skeleton.
-- Target: MySQL 8.0.16+.
-- PROJECT-INIT-1 only reserves the four-table design; no DDL is executed here.
-- DB-INIT-1 will add reviewed DDL, keys, constraints, and indexes.

-- Table: city
-- Future scope: 16 Shandong prefecture-level cities.

-- Table: forecast_model
-- Future seed scope: ECMWF and NOAA.

-- Table: weather_element
-- Future seed scope: T2M and PRECIP.

-- Table: forecast_record
-- Future business key: city_id, model_id, element_id, forecast_time.
