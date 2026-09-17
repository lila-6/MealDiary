package com.example.mealdiary.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 用户实体类
 * 对应 Room 数据库中的 user 表，存储注册用户的账号信息
 * 字段：id(主键自增)、username(用户名)、password(密码)、email(邮箱)
 */
@Entity(tableName = "user")
public class User {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String username;    // 用户名，用于登录标识
    private String password;    // 密码，实际项目中应加密存储
    private String email;       // 邮箱，用于密码找回等功能扩展

    // ===== 构造方法 =====
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // ===== Getter & Setter =====
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}