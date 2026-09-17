package com.example.mealdiary.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mealdiary.R;
import java.io.File;
import java.io.IOException;
import com.example.mealdiary.data.local.SessionManager;
/**
 * 编辑个人信息页面
 * 功能：更换头像（从相册选择）、修改昵称、个人简介、选择性别
 * 所有修改保存到 SharedPreferences，下次打开自动加载
 */
public class EditProfileActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_AVATAR = 300;

    private ImageView ivEditAvatar;
    private EditText etNickname, etBio;
    private Spinner spGender;          // 性别下拉选择器
    private Button btnSave;
    private String avatarPath;         // 头像文件路径
    private SharedPreferences prefs;   // 存储用户设置

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // ===== 绑定控件 =====
        ivEditAvatar = findViewById(R.id.ivEditAvatar);
        etNickname = findViewById(R.id.etNickname);
        etBio = findViewById(R.id.etBio);
        spGender = findViewById(R.id.spGender);
        btnSave = findViewById(R.id.btnSaveProfile);

        // SharedPreferences 用于存储用户设置
        prefs = getSharedPreferences("user_profile", MODE_PRIVATE);

        // ===== 性别下拉选项 =====
        // 提供"保密"、"男"、"女"三个选项
        String[] genders = {"保密", "女", "男"};
        spGender.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, genders));

        // ===== 加载已保存的信息 =====
        loadProfile();

        // ===== 点击头像 → 从相册选择新头像 =====
        ivEditAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_AVATAR);
        });

        // ===== 保存按钮 =====
        btnSave.setOnClickListener(v -> saveProfile());
    }

    /**
     * 从 SharedPreferences 加载已保存的个人信息
     * 使用 userId 作为 key 前缀，确保不同用户的数据隔离
     */
    private void loadProfile() {
        // 获取当前登录用户的 userId
        long userId = SessionManager.getInstance(this).getUserId();

        // 加载昵称，key 为 "userId_nickname"，默认为空
        etNickname.setText(prefs.getString(userId + "_nickname", ""));

        // 加载个人简介，key 为 "userId_bio"，默认为空
        etBio.setText(prefs.getString(userId + "_bio", ""));

        // 加载性别，key 为 "userId_gender"，默认值为"保密"
        String gender = prefs.getString(userId + "_gender", "保密");
        // "保密"索引0，"女"索引1，"男"索引2
        spGender.setSelection("女".equals(gender) ? 1 : ("男".equals(gender) ? 2 : 0));

        // 加载头像路径，key 为 "userId_avatar_path"，默认为空
        avatarPath = prefs.getString(userId + "_avatar_path", "");
        // 如果路径不为空且文件存在，则显示头像
        if (!avatarPath.isEmpty()) {
            File avatarFile = new File(avatarPath);
            if (avatarFile.exists()) {
                ivEditAvatar.setImageURI(Uri.fromFile(avatarFile));
            }
        }
    }

    /**
     * 保存所有修改到 SharedPreferences，成功后返回上一页
     */
    private void saveProfile() {
        try {
            String nickname = etNickname.getText() != null ? etNickname.getText().toString().trim() : "";
            String bio = etBio.getText() != null ? etBio.getText().toString().trim() : "";
            String gender = spGender.getSelectedItem() != null ? spGender.getSelectedItem().toString() : "保密";
            String avatar = avatarPath != null ? avatarPath : "";
            long userId = SessionManager.getInstance(this).getUserId();

            // 写入 SharedPreferences
            prefs.edit()
                    .putString(userId + "_nickname", nickname)
                    .putString(userId + "_bio", bio)
                    .putString(userId + "_gender", gender)
                    .putString(userId + "_avatar_path", avatar)
                    .apply();

            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 处理头像选择返回结果
     * 将选中的图片复制到应用私有目录，并更新预览
     * 支持反复选择，每次都会覆盖之前的头像
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_AVATAR && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                // 先立即显示选中的图片（直接用 Uri 预览，不依赖文件保存）
                ivEditAvatar.setImageURI(selectedImage);

                // 再复制到私有目录，确保重启后头像仍存在
                avatarPath = saveAvatarToStorage(selectedImage);
                if (avatarPath != null) {
                    Toast.makeText(this, "头像已选择，可再次点击更换", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "头像保存失败，请重试", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * 将选中的头像图片复制到应用私有目录，使用唯一文件名防止缓存冲突
     * 文件名格式：avatar_用户ID_时间戳.jpg
     * 每次保存都会生成新文件，确保 Glide 能检测到变化
     */
    private String saveAvatarToStorage(Uri imageUri) {
        try {
            java.io.InputStream input = getContentResolver().openInputStream(imageUri);
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (storageDir != null && !storageDir.exists()) storageDir.mkdirs();

            // 使用 userId + 时间戳生成唯一文件名，避免缓存和覆盖问题
            long userId = SessionManager.getInstance(this).getUserId();
            String fileName = "avatar_" + userId + "_" + System.currentTimeMillis() + ".jpg";
            File avatarFile = new File(storageDir, fileName);

            java.io.FileOutputStream output = new java.io.FileOutputStream(avatarFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > 0) {
                output.write(buffer, 0, len);
            }
            output.close();
            input.close();

            android.util.Log.d("EditProfile", "头像已保存: " + avatarFile.getAbsolutePath());
            return avatarFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}