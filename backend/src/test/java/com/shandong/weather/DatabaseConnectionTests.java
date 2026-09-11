package com.shandong.weather;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = WeatherApplication.class)
class DatabaseConnectionTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void connectsToCourseDatabaseAndPreservesSeedCounts() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setReadOnly(true);

            try (PreparedStatement statement = connection.prepareStatement("SELECT 1");
                 ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
                assertFalse(result.next());
            }

            try (PreparedStatement statement = connection.prepareStatement("SELECT DATABASE()");
                 ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals("shandong_weather", result.getString(1));
                assertFalse(result.next());
            }

            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM city");
                 ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(16L, result.getLong(1));
                assertFalse(result.next());
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT (SELECT COUNT(*) FROM forecast_model) AS model_count, "
                            + "(SELECT COUNT(*) FROM weather_element) AS element_count, "
                            + "(SELECT COUNT(*) FROM forecast_record) AS record_count");
                 ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(2L, result.getLong("model_count"));
                assertEquals(6L, result.getLong("element_count"));
                assertEquals(46080L, result.getLong("record_count"));
                assertFalse(result.next());
            }
        }
    }
}
