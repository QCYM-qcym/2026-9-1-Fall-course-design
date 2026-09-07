package com.shandong.weather;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shandong.weather.entity.WeatherElement;
import com.shandong.weather.mapper.WeatherElementMapper;
import com.shandong.weather.service.WeatherElementService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WeatherElementServiceTest {
    private WeatherElementMapper mapper;
    private WeatherElementService service;

    @BeforeEach
    void setUp() {
        mapper = mock(WeatherElementMapper.class);
        service = new WeatherElementService(mapper);
    }

    @Test
    void returnsAllDatabaseRowsIncludingNonCoreCodesInMapperOrder() {
        when(mapper.selectList(any())).thenReturn(DictionaryTestData.elements());
        var result = service.listAll();
        assertThat(result).extracting(WeatherElement::getId).containsExactly(71L, 80L);
        assertThat(result).extracting(WeatherElement::getElementCode).containsExactly("T2M", "TEMP_ELEMENT");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void requestsUnfilteredQueryWithExplicitAscendingIdOrder() {
        when(mapper.selectList(any())).thenReturn(List.of());
        service.listAll();
        ArgumentCaptor<Wrapper<WeatherElement>> query = ArgumentCaptor.forClass((Class) Wrapper.class);
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
