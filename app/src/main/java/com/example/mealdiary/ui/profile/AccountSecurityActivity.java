package com.example.mealdiary.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mealdiary.R;
import com.example.mealdiary.data.local.database.AppDatabase;
import com.example.mealdiary.data.local.SessionManager;
import com.example.mealdiary.ui.login.LoginActivity;

/**
 * 账号安全页面
 * 提供手机号绑定（展示用）和账户注销功能
 * 账户注销：彻底删除该用户的本地数据（饮食记录、个人信息）和用户账号，跳转登录页
 */
public class AccountSecurityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_security);

        LinearLayout itemBindPhone = findViewById(R.id.itemBindPhone);
        LinearLayout itemDeleteAccount = findViewById(R.id.itemDeleteAccount);

        // 手机号绑定 → 仅提示（需要后端支持）
        itemBindPhone.setOnClickListener(v ->
                Toast.makeText(this, "手机号绑定功能需要后端支持，暂未开放", Toast.LENGTH_SHORT).show());

        // 账户注销 → 确认后彻底删除本地数据及账号
        itemDeleteAccount.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("注销账户")
                        .setMessage("注销后所有本地数据（饮食记录、个人信息）将被永久删除，且该账号将无法再登录。\n确定要继续吗？")
                        .setPositiveButton("确定注销", (dialog, which) -> deleteAccount())
                        .setNegativeButton("取消", null)
                        .show());
    }

    /**
     * 彻底注销账户：
     * 1. 在后台线程删除当前用户的所有饮食记录
     * 2. 删除该用户账号（从Room的User表）
     * 3. 清除登录状态和所有SharedPreferences
     * 4. 跳转到登录页
     */
    private void deleteAccount() {
        // 显示进度提示
        Toast.makeText(this, "正在注销...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            long userId = SessionManager.getInstance(this).getUserId();

            // 1. 删除该用户的所有饮食记录
            db.mealDao().deleteRecordsByUserId(userId);  // 需要在MealDao中新增此方法

            // 2. 删除用户账号
            db.userDao().deleteUserById(userId);         // 需要在UserDao中新增此方法

            // 3. 清除SharedPreferences（登录状态和个人信息）
            SessionManager.getInstance(this).logout();
            this.getSharedPreferences("user_profile", MODE_PRIVATE).edit().clear().apply();
            this.getSharedPreferences("meal_reminders", MODE_PRIVATE).edit().clear().apply();

            // 4. 切回主线程提示并跳转
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(AccountSecurityActivity.this, "账户已彻底注销", Toast.LENGTH_SHORT).show();
                // 跳转到登录页，清除活动栈
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }).start();
    }
}