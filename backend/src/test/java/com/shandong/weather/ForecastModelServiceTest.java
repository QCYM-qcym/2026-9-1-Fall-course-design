package com.shandong.weather;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shandong.weather.entity.ForecastModel;
import com.shandong.weather.mapper.ForecastModelMapper;
import com.shandong.weather.service.ForecastModelService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ForecastModelServiceTest {
    private ForecastModelMapper mapper;
    private ForecastModelService service;

    @BeforeEach
    void setUp() {
        mapper = mock(ForecastModelMapper.class);
        service = new ForecastModelService(mapper);
    }

    @Test
    void returnsAllDatabaseRowsIncludingNonCoreCodesInMapperOrder() {
        when(mapper.selectList(any())).thenReturn(DictionaryTestData.models());
        var result = service.listAll();
        assertThat(result).extracting(ForecastModel::getId).containsExactly(41L, 90L);
        assertThat(result).extracting(ForecastModel::getModelCode).containsExactly("ECMWF", "TEMP_MODEL");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void requestsUnfilteredQueryWithExplicitAscendingIdOrder() {
        when(mapper.selectList(any())).thenReturn(List.of());
        service.listAll();
        ArgumentCaptor<Wrapper<ForecastModel>> query = ArgumentCaptor.forClass((Class) Wrapper.class);
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
