package com.shandong.weather;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shandong.weather.entity.City;
import com.shandong.weather.mapper.CityMapper;
import com.shandong.weather.service.CityService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CityServiceTest {
    private CityMapper mapper;
    private CityService service;

    @BeforeEach
    void setUp() {
        mapper = mock(CityMapper.class);
        service = new CityService(mapper);
    }

    @Test
    void returnsAllDatabaseRowsIncludingNonCoreCodesInMapperOrder() {
        when(mapper.selectList(any())).thenReturn(DictionaryTestData.cities());
        var result = service.listAll();
        assertThat(result).extracting(City::getId).containsExactly(8L, 40L);
        assertThat(result).extracting(City::getCityCode).containsExactly("JINAN", "TEMP_CITY");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void requestsUnfilteredQueryWithExplicitAscendingIdOrder() {
        when(mapper.selectList(any())).thenReturn(List.of());
        service.listAll();
        ArgumentCaptor<Wrapper<City>> query = ArgumentCaptor.forClass((Class) Wrapper.class);
        verify(mapper).selectList(query.capture());
        // Assert the real Service's query contract, not the mock's returned ordering.
        assertThat(query.getValue().getSqlSegment().trim()).isEqualTo("ORDER BY id ASC");
        assertThat(query.getValue().getEntity()).isNull();
        assertThat(query.getValue().getSqlSelect()).isNull();
    }

    @Test
    void emptyTableRemainsAnEmptyList() {
        when(mapper.selectList(any())).thenReturn(List.of());
        assertThat(service.listAll()).isNotNull().isEmpty();
    }
}
