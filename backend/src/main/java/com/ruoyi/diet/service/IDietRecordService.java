package com.ruoyi.diet.service;

import java.util.List;
import com.ruoyi.diet.domain.DietRecord;

/**
 * 饮食记录Service接口
 * 
 * @author ruoyi
 * @date 2026-05-15
 */
public interface IDietRecordService 
{
    /**
     * 查询饮食记录
     * 
     * @param id 饮食记录主键
     * @return 饮食记录
     */
    public DietRecord selectDietRecordById(Long id);

    /**
     * 查询饮食记录列表
     * 
     * @param dietRecord 饮食记录
     * @return 饮食记录集合
     */
    public List<DietRecord> selectDietRecordList(DietRecord dietRecord);

    /**
     * 新增饮食记录
     * 
     * @param dietRecord 饮食记录
     * @return 结果
     */
    public int insertDietRecord(DietRecord dietRecord);

    /**
     * 修改饮食记录
     * 
     * @param dietRecord 饮食记录
     * @return 结果
     */
    public int updateDietRecord(DietRecord dietRecord);

    /**
     * 批量删除饮食记录
     * 
     * @param ids 需要删除的饮食记录主键集合
     * @return 结果
     */
    public int deleteDietRecordByIds(String ids);

    /**
     * 删除饮食记录信息
     * 
     * @param id 饮食记录主键
     * @return 结果
     */
    public int deleteDietRecordById(Long id);
}
