package com.example.mealdiary.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.mealdiary.data.local.entity.User;

/**
 * 用户数据访问对象（DAO）
 * 提供对 user 表的数据库操作方法
 * 包含：注册插入用户、登录验证查询、检查用户名是否已存在
 */
@Dao
public interface UserDao {

    // 注册时插入新用户，返回自增 id
    @Insert
    long insertUser(User user);

    // 登录验证：根据用户名和密码查询用户
    @Query("SELECT * FROM user WHERE username = :username AND password = :password LIMIT 1")
    User login(String username, String password);

    // 检查用户名是否已存在（注册时防重复）
    @Query("SELECT * FROM user WHERE username = :username LIMIT 1")
    User getUserByUsername(String username);

    /**
     * 删除指定ID的用户
     * 用于账户注销
     */
    @Query("DELETE FROM user WHERE id = :userId")
    void deleteUserById(long userId);
}