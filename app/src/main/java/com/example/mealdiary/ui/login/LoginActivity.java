package com.example.mealdiary.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mealdiary.R;
import com.example.mealdiary.data.local.database.AppDatabase;
import com.example.mealdiary.data.local.SessionManager;
import com.example.mealdiary.data.local.entity.User;
import com.example.mealdiary.ui.MainActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 登录页面 Activity
 * 功能：
 * 1. 输入用户名和密码，点击"登录"按钮进行验证
 * 2. 验证通过后保存登录状态，跳转到 MainActivity
 * 3. 点击"去注册"跳转到注册页面
 * 4. 已登录用户下次启动时自动跳过登录页
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvGoRegister;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 检查是否已登录，如果已登录直接跳转到主界面
        SessionManager session = SessionManager.getInstance(this);
        if (session.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etLoginUsername);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoRegister = findViewById(R.id.tvGoRegister);

        // 登录按钮点击事件
        btnLogin.setOnClickListener(v -> performLogin());

        // 跳转注册页面
        tvGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    /**
     * 执行登录验证
     * 在后台线程查询数据库，验证用户名密码是否匹配
     */
    private void performLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 后台线程开始
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);//拿到数据库实例后，可以通过它获取 DAO 操作数据
            User user = db.userDao().login(username, password);//在 user 表里查是否有匹配的用户名和密码

            //切回主线程
            runOnUiThread(() -> {
                if (user != null) {
                    // 查到匹配的用户 → 登录成功
                    // 登录成功，保存会话，跳转主界面
                    SessionManager.getInstance(this).saveLogin(user.getId(), user.getUsername());
                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    // 查不到 → 用户名或密码错误
                    Toast.makeText(this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}