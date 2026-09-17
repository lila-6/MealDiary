package com.ruoyi.diet.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.diet.mapper.DietRecordMapper;
import com.ruoyi.diet.domain.DietRecord;
import com.ruoyi.diet.service.IDietRecordService;
import com.ruoyi.common.core.text.Convert;

/**
 * 饮食记录Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-15
 */
@Service
public class DietRecordServiceImpl implements IDietRecordService 
{
    @Autowired
    private DietRecordMapper dietRecordMapper;

    /**
     * 查询饮食记录
     * 
     * @param id 饮食记录主键
     * @return 饮食记录
     */
    @Override
    public DietRecord selectDietRecordById(Long id)
    {
        return dietRecordMapper.selectDietRecordById(id);
    }

    /**
     * 查询饮食记录列表
     * 
     * @param dietRecord 饮食记录
     * @return 饮食记录
     */
    @Override
    public List<DietRecord> selectDietRecordList(DietRecord dietRecord)
    {
        return dietRecordMapper.selectDietRecordList(dietRecord);
    }

    /**
     * 新增饮食记录
     * 
     * @param dietRecord 饮食记录
     * @return 结果
     */
    @Override
    public int insertDietRecord(DietRecord dietRecord)
    {
        dietRecord.setCreateTime(DateUtils.getNowDate());
        return dietRecordMapper.insertDietRecord(dietRecord);
    }

    /**
     * 修改饮食记录
     * 
     * @param dietRecord 饮食记录
     * @return 结果
     */
    @Override
    public int updateDietRecord(DietRecord dietRecord)
    {
        return dietRecordMapper.updateDietRecord(dietRecord);
    }

    /**
     * 批量删除饮食记录
     * 
     * @param ids 需要删除的饮食记录主键
     * @return 结果
     */
    @Override
    public int deleteDietRecordByIds(String ids)
    {
        return dietRecordMapper.deleteDietRecordByIds(Convert.toStrArray(ids));
    }

    /**
     * 删除饮食记录信息
     * 
     * @param id 饮食记录主键
     * @return 结果
     */
    @Override
    public int deleteDietRecordById(Long id)
    {
        return dietRecordMapper.deleteDietRecordById(id);
    }
}
