package com.example.mealdiary.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 饮食记录实体类（Room 数据库表映射）
 *
 * 对应数据库中的 meal_record 表，每个 MealRecord 对象代表表中的一条饮食记录
 *
 * 字段说明：
 * - id：主键，自增，唯一标识一条记录
 * - userId：用户ID，用于区分不同用户的数据，实现数据隔离
 * - mealType：三餐类型（breakfast 早餐 / lunch 午餐 / dinner 晚餐 / snack 加餐）
 * - foodName：食物名称，用户手动输入
 * - imagePath：图片本地存储路径（选择后复制到应用私有目录）
 * - audioPath：录音文件本地存储路径（MediaRecorder 录制 3gp 格式）
 * - note：文字备注，可选
 * - createTime：记录创建时间戳（毫秒），支持补录历史日期
 * - syncStatus：同步状态（0=未同步待上传，1=已同步至若依后端）
 *
 * 使用 @Entity 注解声明为 Room 实体类，@PrimaryKey 标记主键并设置自增
 */

@Entity(tableName = "meal_record")
public class MealRecord {

    @PrimaryKey(autoGenerate = true)
    private long id; /** 主键ID，自增，唯一标识一条记录 */

    private long userId;          /** 用户ID，与若依后端对应，用于数据隔离 */
    private String mealType;       /** 三餐类型：breakfast 早餐 / lunch 午餐 / dinner 晚餐 / snack 加餐 */
    private String foodName;       /** 食物名称，用户手动输入 */
    private String imagePath;       /** 图片本地存储路径 */
    private String audioPath;        /** 录音文件本地存储路径 */
    private String note;           /** 文字备注，可选 */
    private long createTime;         /** 记录创建时间戳（毫秒），支持补录历史日期 */
    private int syncStatus;        /** 同步状态：0=未同步（待上传），1=已同步（已上传至若依后端） */

    /**
     * 创建一条饮食记录
     * @param userId 用户ID
     * @param mealType 三餐类型
     * @param foodName 食物名称
     * @param imagePath 图片路径
     * @param audioPath 录音路径
     * @param note 备注
     * @param createTime 记录时间戳
     * @param syncStatus 同步状态（0=未同步）
     */

    // ===== 构造方法 =====
    public MealRecord(long userId, String mealType, String foodName,
                      String imagePath, String audioPath, String note,
                      long createTime, int syncStatus) {
        this.userId = userId;
        this.mealType = mealType;
        this.foodName = foodName;
        this.imagePath = imagePath;
        this.audioPath = audioPath;
        this.note = note;
        this.createTime = createTime;
        this.syncStatus = syncStatus;
    }

    // ===== Getter & Setter =====
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    public int getSyncStatus() { return syncStatus; }
    public void setSyncStatus(int syncStatus) { this.syncStatus = syncStatus; }
}