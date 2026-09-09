package com.shandong.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shandong.weather.common.BusinessException;
import com.shandong.weather.common.ManagementValidation;
import com.shandong.weather.dto.ForecastModelWriteRequest;
import com.shandong.weather.entity.ForecastModel;
import com.shandong.weather.entity.ForecastRecord;
import com.shandong.weather.mapper.ForecastModelMapper;
import com.shandong.weather.mapper.ForecastRecordMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForecastModelService {
    @Autowired
    private ForecastRecordMapper records;
    private final ForecastModelMapper mapper;

    public ForecastModelService(ForecastModelMapper mapper) {
        this.mapper = mapper;
    }

    public List<ForecastModel> listAll() {
        return mapper.selectList(new QueryWrapper<ForecastModel>().orderByAsc("id"));
    }

    @Transactional
    public ForecastModel create(ForecastModelWriteRequest request) {
        unique(request.modelCode(), null);
        ForecastModel model = from(request);
        mapper.insert(model);
        return model;
    }

    @Transactional
    public ForecastModel update(Long id, ForecastModelWriteRequest request) {
        ForecastModel old = require(id);
        if (!old.getModelCode().equals(request.modelCode()) && referenced(id))
            throw new BusinessException(409, "referenced model code cannot change");
        unique(request.modelCode(), id);
        ForecastModel model = from(request); model.setId(id);
        if (mapper.updateById(model) != 1) throw new BusinessException(404, "model not found");
        return model;
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        if (referenced(id)) throw new BusinessException(409, "model is referenced");
        if (mapper.deleteById(id) != 1) throw new BusinessException(404, "model not found");
    }

    private boolean referenced(Long id) {
        return records.selectCount(new QueryWrapper<ForecastRecord>().eq("model_id", id)) > 0;
    }

    private void unique(String code, Long id) {
        var query = new QueryWrapper<ForecastModel>().eq("model_code", code);
        if (id != null) query.ne("id", id);
        if (mapper.selectCount(query) > 0) throw new BusinessException(409, "model code already exists");
    }

    private ForecastModel require(Long id) {
        ManagementValidation.id(id);
        ForecastModel model = mapper.selectOne(new QueryWrapper<ForecastModel>().eq("id", id).last("FOR UPDATE"));
        if (model == null) throw new BusinessException(404, "model not found");
        return model;
    }

    private ForecastModel from(ForecastModelWriteRequest request) {
        ForecastModel model = new ForecastModel();
        model.setModelCode(request.modelCode()); model.setModelName(request.modelName().trim());
        model.setDescription(request.description());
        return model;
    }
}
