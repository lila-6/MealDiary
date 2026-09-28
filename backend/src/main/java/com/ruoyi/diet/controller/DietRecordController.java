package com.ruoyi.diet.controller;

import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.diet.domain.DietRecord;
import com.ruoyi.diet.service.IDietRecordService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 饮食记录Controller
 * 
 * @author ruoyi
 * @date 2026-05-15
 */
import com.ruoyi.common.annotation.Anonymous;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestBody;
import java.text.SimpleDateFormat;
@Anonymous
@Controller
@RequestMapping("/diet/record")
public class DietRecordController extends BaseController
{
    private String prefix = "diet/record";

    @Autowired
    private IDietRecordService dietRecordService;


    @GetMapping()
    public String record()
    {
        return prefix + "/record";
    }

    /**
     * 查询饮食记录列表
     */

    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(DietRecord dietRecord)
    {
        startPage();
        List<DietRecord> list = dietRecordService.selectDietRecordList(dietRecord);
        return getDataTable(list);
    }

    /**
     * 导出饮食记录列表
     */

    @Log(title = "饮食记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(DietRecord dietRecord)
    {
        List<DietRecord> list = dietRecordService.selectDietRecordList(dietRecord);
        ExcelUtil<DietRecord> util = new ExcelUtil<DietRecord>(DietRecord.class);
        return util.exportExcel(list, "饮食记录数据");
    }

    /**
     * 新增饮食记录
     */

    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存饮食记录
     */

    @Log(title = "饮食记录", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@RequestBody Map<String, Object> params)
    {
        DietRecord dietRecord = new DietRecord();
        dietRecord.setUserId(Long.valueOf(params.get("userId").toString()));
        dietRecord.setMealType((String) params.get("mealType"));
        dietRecord.setFoodName((String) params.get("foodName"));
        dietRecord.setImagePath((String) params.get("imagePath"));
        dietRecord.setAudioPath((String) params.get("audioPath"));
        dietRecord.setNote((String) params.get("note"));

        try {
            String createTimeStr = (String) params.get("createTime");
            if (createTimeStr != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dietRecord.setCreateTime(sdf.parse(createTimeStr));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (params.get("syncStatus") != null) {
            dietRecord.setSyncStatus(Long.valueOf(params.get("syncStatus").toString()));
        }

        return toAjax(dietRecordService.insertDietRecord(dietRecord));
    }
    /**
     * 修改饮食记录
     */

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap)
    {
        DietRecord dietRecord = dietRecordService.selectDietRecordById(id);
        mmap.put("dietRecord", dietRecord);
        return prefix + "/edit";
    }

    /**
     * 修改保存饮食记录
     */

    @Log(title = "饮食记录", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(DietRecord dietRecord)
    {
        return toAjax(dietRecordService.updateDietRecord(dietRecord));
    }

    /**
     * 删除饮食记录
     */

    @Log(title = "饮食记录", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(dietRecordService.deleteDietRecordByIds(ids));
    }
}
