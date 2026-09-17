package com.example.mealdiary.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mealdiary.data.local.entity.MealRecord;

import java.util.List;

/**
 * 饮食记录数据访问对象（DAO）
 *
 * 这是一个接口，只声明方法，具体 SQL 执行代码由 Room 框架编译时自动生成实现类
 * 每个方法对应一条 SQL 语句，通过注解标记操作类型：
 * - @Insert：插入数据
 * - @Update：更新数据
 * - @Query：自定义 SQL 查询/删除
 *
 * 所有数据库操作都需要在后台线程调用，Room 禁止主线程访问
 */
@Dao
public interface MealDao {

    /**
     * 插入一条饮食记录
     * @param record MealRecord 对象
     * @return 插入后的自增 id
     */
    @Insert
    long insert(MealRecord record);

    /**
     * 更新一条饮食记录
     * 通常用于修改 syncStatus（同步后标记为1）或软删除标记
     * @param record 已修改的 MealRecord 对象
     */
    @Update
    void update(MealRecord record);

    /**
     * 查询所有饮食记录（不过滤用户，调试用）
     * 按创建时间倒序排列，最新的在前面
     */
    @Query("SELECT * FROM meal_record ORDER BY createTime DESC")
    List<MealRecord> getAllRecords();

    /**
     * 查询所有未同步的记录（不过滤用户）
     * SyncService 通过此方法获取待上传数据
     * @return syncStatus=0 的记录列表
     */
    @Query("SELECT * FROM meal_record WHERE syncStatus = 0")
    List<MealRecord> getUnsyncedRecords();

    /**
     * 根据 id 查询单条记录
     * DetailActivity 查看详情时使用
     * @param id 记录的主键 id
     * @return 对应的 MealRecord 对象，不存在则返回 null
     */
    @Query("SELECT * FROM meal_record WHERE id = :id")
    MealRecord getRecordById(long id);

    /**
     * 根据 id 删除单条记录
     * DetailActivity 删除记录时使用
     * @param id 记录的主键 id
     */
    @Query("DELETE FROM meal_record WHERE id = :id")
    void deleteById(long id);

    /**
     * 按用户ID查询该用户的所有记录
     * 实现数据隔离：不同用户只能看到自己的记录
     * 按创建时间倒序排列
     * @param userId 当前登录用户的 id
     * @return 该用户的所有饮食记录
     */
    @Query("SELECT * FROM meal_record WHERE userId = :userId ORDER BY createTime DESC")
    List<MealRecord> getRecordsByUserId(long userId);

    /**
     * 按用户ID查询该用户所有未同步的记录
     * SyncService 使用此方法获取当前用户待上传的数据
     * @param userId 当前登录用户的 id
     * @return 该用户所有 syncStatus=0 的记录
     */
    @Query("SELECT * FROM meal_record WHERE userId = :userId AND syncStatus = 0")
    List<MealRecord> getUnsyncedRecordsByUserId(long userId);

    /**
     * 按用户ID和日期查询记录
     * DiaryFragment 日历点击某一天时，加载当天的饮食记录
     * 使用 SQLite 的 date() 函数将时间戳转换为日期字符串进行比对
     * @param userId 当前登录用户的 id
     * @param date 日期字符串，格式 yyyy-MM-dd
     * @return 该用户指定日期的所有记录
     */
    @Query("SELECT * FROM meal_record WHERE userId = :userId AND date(createTime/1000, 'unixepoch', 'localtime') = :date ORDER BY createTime DESC")
    List<MealRecord> getRecordsByDate(long userId, String date);

    /**
     * 删除指定用户的所有饮食记录
     * 用于账户注销功能，彻底清除该用户的数据
     * @param userId 要注销的用户 id
     */
    @Query("DELETE FROM meal_record WHERE userId = :userId")
    void deleteRecordsByUserId(long userId);
}