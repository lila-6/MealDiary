package com.ruoyi.diet.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 饮食记录对象 diet_record
 * 
 * @author ruoyi
 * @date 2026-05-15
 */
public class DietRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 餐类：breakfast/lunch/dinner/snack */
    @Excel(name = "餐类：breakfast/lunch/dinner/snack")
    private String mealType;

    /** 食物名称 */
    @Excel(name = "食物名称")
    private String foodName;

    /** 图片路径 */
    @Excel(name = "图片路径")
    private String imagePath;

    /** 录音路径 */
    @Excel(name = "录音路径")
    private String audioPath;

    /** 文字备注 */
    @Excel(name = "文字备注")
    private String note;

    /** 同步状态：0未同步，1已同步 */
    @Excel(name = "同步状态：0未同步，1已同步")
    private Long syncStatus;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setMealType(String mealType) 
    {
        this.mealType = mealType;
    }

    public String getMealType() 
    {
        return mealType;
    }

    public void setFoodName(String foodName) 
    {
        this.foodName = foodName;
    }

    public String getFoodName() 
    {
        return foodName;
    }

    public void setImagePath(String imagePath) 
    {
        this.imagePath = imagePath;
    }

    public String getImagePath() 
    {
        return imagePath;
    }

    public void setAudioPath(String audioPath) 
    {
        this.audioPath = audioPath;
    }

    public String getAudioPath() 
    {
        return audioPath;
    }

    public void setNote(String note) 
    {
        this.note = note;
    }

    public String getNote() 
    {
        return note;
    }

    public void setSyncStatus(Long syncStatus) 
    {
        this.syncStatus = syncStatus;
    }

    public Long getSyncStatus() 
    {
        return syncStatus;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("mealType", getMealType())
            .append("foodName", getFoodName())
            .append("imagePath", getImagePath())
            .append("audioPath", getAudioPath())
            .append("note", getNote())
            .append("createTime", getCreateTime())
            .append("syncStatus", getSyncStatus())
            .toString();
    }
}
