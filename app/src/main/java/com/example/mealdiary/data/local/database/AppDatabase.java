package com.example.mealdiary.data.local.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.mealdiary.data.local.dao.MealDao;
import com.example.mealdiary.data.local.dao.UserDao;
import com.example.mealdiary.data.local.entity.MealRecord;
import com.example.mealdiary.data.local.entity.User;

/**
 * Room 数据库管理类（单例模式）
 * 使用 AppDatabase 类来获取数据库实例
 * 使用 RoomDatabase.Builder 创建数据库
 * 数据库名：meal_diary_db
 * 包含两张表：meal_record（饮食记录）、user（用户）
 * 版本号：2
 */
@Database(entities = {MealRecord.class, User.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    // 声明两个 DAO 抽象方法,abstract 表示"我只是声明，Room 会帮我实现"。
    public abstract MealDao mealDao();// 声明：我要操作 meal_record 表
    public abstract UserDao userDao();// 声明：我要操作 user 表

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "meal_diary_db"
                            )
                            .fallbackToDestructiveMigration()  // 数据库升级时重建（开发阶段使用）
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}