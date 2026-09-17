package com.example.mealdiary.data.local;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 登录状态管理器
 * 使用 SharedPreferences 保存当前登录用户信息
 * 提供登录/登出/判断是否已登录/获取当前用户ID等方法
 * 全局单例，各页面可随时获取登录状态
 */
public class SessionManager {

    private static final String PREF_NAME = "meal_diary_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private static SessionManager instance;

    // 私有构造，单例模式
    private SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    // 保存登录状态
    public void saveLogin(long userId, String username) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    // 判断是否已登录
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // 获取当前登录用户ID
    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    // 获取当前登录用户名
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    // 退出登录
    public void logout() {
        editor.clear();
        editor.apply();
    }
}