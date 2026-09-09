package com.shandong.weather;

import com.shandong.weather.entity.ForecastModel;
import com.shandong.weather.mapper.ForecastRecordMapper;
import com.shandong.weather.mapper.ForecastModelMapper;
import java.util.Map;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ManagementMapperBindingTest {
    @Autowired SqlSessionFactory factory;

    @Test void recordCrudAndExistingXmlShareOneNamespace() {
        for (String method : new String[]{"insert", "updateById", "deleteById", "selectList", "selectCount",
                "selectWorkbenchData", "selectTrendData", "selectComparisonData"}) {
            assertThat(factory.getConfiguration().hasStatement(ForecastRecordMapper.class.getName() + "." + method)).isTrue();
        }
    }

    @Test void nullDescriptionIsIncludedInUpdateSql() {
        var model = new ForecastModel();
        model.setId(80L); model.setModelCode("TEMP_MODEL"); model.setModelName("Temporary");
        var sql = factory.getConfiguration().getMappedStatement(ForecastModelMapper.class.getName() + ".updateById")
                .getBoundSql(Map.of("et", model)).getSql().replaceAll("\\s+", " ");
        assertThat(sql).contains("description=?");
    }
    @Test void fullListUsesConsistentReadOnlySnapshot() throws Exception {
        var tx = com.shandong.weather.service.ForecastRecordService.class.getMethod("listAll")
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isTrue();
        assertThat(tx.isolation()).isEqualTo(org.springframework.transaction.annotation.Isolation.REPEATABLE_READ);
    }
}
