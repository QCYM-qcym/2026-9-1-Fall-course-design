-- 山东省气象预报数据可视化系统数据库初始化脚本
-- Target: MySQL 8.0.16+; source files and DDL are UTF-8.
-- DB-INIT-1: executable four-table schema for the frozen Database V2 design.
-- The script is rerunnable and drops/recreates only the four application tables.

CREATE DATABASE IF NOT EXISTS shandong_weather
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE shandong_weather;

SET NAMES utf8mb4;

-- Drop dependents before referenced dictionary tables.
DROP TABLE IF EXISTS forecast_record;
DROP TABLE IF EXISTS weather_element;
DROP TABLE IF EXISTS forecast_model;
DROP TABLE IF EXISTS city;

CREATE TABLE city (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  city_code VARCHAR(32) NOT NULL,
  city_name VARCHAR(50) NOT NULL,
  longitude DECIMAL(10,6) NOT NULL,
  latitude DECIMAL(10,6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_city_city_code UNIQUE (city_code),
  CONSTRAINT chk_city_longitude CHECK (longitude BETWEEN -180.000000 AND 180.000000),
  CONSTRAINT chk_city_latitude CHECK (latitude BETWEEN -90.000000 AND 90.000000)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='山东省地级市字典';

CREATE TABLE forecast_model (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  model_code VARCHAR(32) NOT NULL,
  model_name VARCHAR(50) NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_forecast_model_model_code UNIQUE (model_code)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='预报模型字典';

CREATE TABLE weather_element (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  element_code VARCHAR(32) NOT NULL,
  element_name VARCHAR(50) NOT NULL,
  unit VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_weather_element_element_code UNIQUE (element_code)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='气象要素字典';

CREATE TABLE forecast_record (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  city_id BIGINT UNSIGNED NOT NULL,
  model_id BIGINT UNSIGNED NOT NULL,
  element_id BIGINT UNSIGNED NOT NULL,
  forecast_time DATETIME NOT NULL,
  value DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_forecast_record_business
    UNIQUE (city_id, model_id, element_id, forecast_time),
  KEY idx_forecast_record_workbench (model_id, element_id, forecast_time),
  KEY idx_forecast_record_element_id (element_id),
  CONSTRAINT fk_forecast_record_city
    FOREIGN KEY (city_id) REFERENCES city (id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT fk_forecast_record_model
    FOREIGN KEY (model_id) REFERENCES forecast_model (id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT fk_forecast_record_element
    FOREIGN KEY (element_id) REFERENCES weather_element (id)
    ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='气象预报记录；PRECIP 表示不重叠三小时时段量';

-- AUTH-ROLE-1: independent authentication table; meteorological DDL above is unchanged.
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  password_hash VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  role VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  CONSTRAINT uq_sys_user_username UNIQUE (username),
  CONSTRAINT chk_sys_user_role CHECK (role IN ('USER', 'ADMIN')),
  CONSTRAINT chk_sys_user_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Session authentication users; roles originate only from this table';
