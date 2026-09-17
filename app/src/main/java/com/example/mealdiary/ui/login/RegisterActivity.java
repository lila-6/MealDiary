package com.example.mealdiary.ui.login;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mealdiary.R;
import com.example.mealdiary.data.local.database.AppDatabase;
import com.example.mealdiary.data.local.entity.User;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 注册页面 Activity
 *
 * 功能流程：
 * 1. 用户输入用户名、密码、确认密码、邮箱（可选）
 * 2. 验证输入合法性（用户名和密码不为空、两次密码一致）
 * 3. 后台线程查询数据库，检查用户名是否已被注册（UserDao.getUserByUsername）
 * 4. 用户名可用则插入新用户（UserDao.insertUser），注册成功返回登录页
 * 5. 用户名已存在则提示用户换一个
 *
 * 数据存储：用户信息存入 Room 数据库的 user 表
 */
public class RegisterActivity extends AppCompatActivity {

    // ===== UI控件 =====
    private EditText etUsername;          // 用户名输入框
    private EditText etPassword;          // 密码输入框
    private EditText etConfirmPassword;   // 确认密码输入框
    private EditText etEmail;             // 邮箱输入框（可选）
    private Button btnRegister;           // 注册按钮
    private TextView tvGoLogin;           // "已有账号？去登录" 文字链接

    // 后台线程池，用于数据库操作（避免主线程访问 Room 闪退）
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // ===== 绑定控件 =====
        etUsername = findViewById(R.id.etRegUsername);
        etPassword = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        etEmail = findViewById(R.id.etRegEmail);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoLogin = findViewById(R.id.tvGoLogin);

        // ===== 注册按钮点击 → 执行注册逻辑 =====
        btnRegister.setOnClickListener(v -> performRegister());

        // ===== "已有账号？去登录" → 关闭当前页面，返回登录页 =====
        tvGoLogin.setOnClickListener(v -> finish());
    }

    /**
     * 执行注册逻辑
     *
     * 步骤：
     * 1. 获取用户输入并去除首尾空格
     * 2. 验证输入合法性（空值检查、两次密码一致性检查）
     * 3. 后台线程操作数据库：
     *    a. 调用 UserDao.getUserByUsername 查重
     *    b. 用户名可用则调用 UserDao.insertUser 插入新用户
     * 4. 切回主线程显示结果并跳转
     */
    private void performRegister() {
        // 获取输入值，trim() 去除首尾空格
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        // ===== 输入验证 =====
        // 用户名和密码不能为空
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        // 两次密码必须一致
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "两次密码输入不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== 后台线程操作数据库 =====
        // Room 禁止主线程访问数据库，所以用 executor.execute 切到后台线程
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);  // 获取数据库实例

            // 检查用户名是否已被注册（查重）
            User existUser = db.userDao().getUserByUsername(username);
            if (existUser != null) {
                // 用户名已存在，切回主线程提示用户
                runOnUiThread(() ->
                        Toast.makeText(this, "用户名已存在，请换一个", Toast.LENGTH_SHORT).show());
                return;  // 终止注册流程
            }

            // 用户名可用，创建新用户对象并插入数据库
            User newUser = new User(username, password, email);
            long userId = db.userDao().insertUser(newUser);  // insertUser 返回自增 id

            // 切回主线程显示结果
            runOnUiThread(() -> {
                if (userId > 0) {
                    // 插入成功（id > 0 表示有数据写入）
                    Toast.makeText(this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                    finish();  // 关闭注册页，返回登录页
                } else {
                    // 理论上不会走到这里，保留作为容错
                    Toast.makeText(this, "注册失败，请重试", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * 页面销毁时关闭线程池，释放资源
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}