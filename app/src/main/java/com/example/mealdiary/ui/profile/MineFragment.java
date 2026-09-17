package com.example.mealdiary.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.mealdiary.R;
import com.example.mealdiary.data.local.database.AppDatabase;
import com.example.mealdiary.data.local.entity.MealRecord;
import com.example.mealdiary.data.local.SessionManager;
import com.example.mealdiary.ui.login.LoginActivity;
import java.io.File;
import java.util.List;

/**
 * 我的页面 Fragment
 * 显示：头像/昵称/简介 + 数据看板（总记录/本周/待同步） + 功能入口
 * 支持编辑个人信息后自动同步显示
 */
public class MineFragment extends Fragment {

    private static final String ARG_USERNAME = "username";
    private static final String ARG_EMAIL = "email";

    // ===== UI控件 =====
    private ImageView ivAvatar;          // 头像
    private TextView tvNickname;         // 昵称
    private TextView tvBio;              // 个人简介
    private TextView tvEditProfile;      // 编辑个人信息入口

    // 数据看板控件
    private TextView tvTotalRecords;     // 总记录数
    private TextView tvWeekRecords;      // 本周记录数
    private TextView tvUnsyncedRecords;  // 待同步记录数

    /**
     * 创建 MineFragment 实例，传入硬编码的默认用户名和邮箱
     * 用于在 MainActivity 中初始化"我的"页面
     */
    public static MineFragment newInstance(String username, String email) {
        MineFragment fragment = new MineFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);

        // ===== 绑定个人信息控件 =====
        ivAvatar = view.findViewById(R.id.ivAvatar);          // 头像
        tvNickname = view.findViewById(R.id.tvNickname);      // 昵称
        tvBio = view.findViewById(R.id.tvBio);                // 个人简介

        // ===== 绑定功能列表控件 =====
        LinearLayout itemGeneral = view.findViewById(R.id.itemGeneral);              // 通用设置
        LinearLayout itemProviderTest = view.findViewById(R.id.itemProviderTest);    // ContentProvider演示
        LinearLayout itemLogout = view.findViewById(R.id.itemLogout);               // 退出登录
        LinearLayout itemAccountSecurity = view.findViewById(R.id.itemAccountSecurity); // 账号安全

        // ===== 绑定数据看板控件 =====
        tvTotalRecords = view.findViewById(R.id.tvTotalRecords);
        tvWeekRecords = view.findViewById(R.id.tvWeekRecords);
        tvUnsyncedRecords = view.findViewById(R.id.tvUnsyncedRecords);

        // ===== 设置昵称和简介 =====
        // 优先从 SharedPreferences 加载编辑过的个人信息
        // 如果没有编辑过，则使用默认值：用户名 + 默认简介
        SessionManager session = SessionManager.getInstance(requireContext());
        long userId = session.getUserId();
        SharedPreferences prefs = requireContext().getSharedPreferences("user_profile", Context.MODE_PRIVATE);

        // 加载昵称：编辑过就用编辑的，否则用登录用户名
        String savedNickname = prefs.getString(userId + "_nickname", "");
        if (!savedNickname.isEmpty()) {
            tvNickname.setText(savedNickname);
        } else if (session.isLoggedIn()) {
            tvNickname.setText(session.getUsername());
        }

        // 加载简介：编辑过就用编辑的，否则用默认文案
        String savedBio = prefs.getString(userId + "_bio", "");
        if (!savedBio.isEmpty()) {
            tvBio.setText(savedBio);
        } else {
            tvBio.setText("这个人很懒，什么都没写");
        }



        // 功能列表 → 编辑个人信息 → 跳转到编辑页面
        LinearLayout itemEditProfile = view.findViewById(R.id.itemEditProfile);
        itemEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        // ===== 账号安全 → 跳转到账号安全页面 =====
        itemAccountSecurity.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AccountSecurityActivity.class);
            startActivity(intent);
        });

        // ===== 通用设置 → 弹窗提示（演示版） =====
        itemGeneral.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("通用设置")
                        .setMessage("外观主题切换 | 多语言支持\n\n（演示版，功能待扩展）")
                        .setPositiveButton("确定", null)
                        .show());

        // ===== ContentProvider 数据共享演示 =====
        itemProviderTest.setOnClickListener(v -> testContentProvider());

        // ===== 退出登录 =====
        itemLogout.setOnClickListener(v -> {
            session.logout();  // 清除登录状态
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });

        return view;
    }

    /**
     * 页面恢复时刷新数据看板和个人信息
     * 确保从编辑页面返回后，昵称和头像能自动更新
     */
    @Override
    public void onResume() {
        super.onResume();
        loadDataPanel();       // 加载数据看板（总记录/本周/待同步）
        loadProfileInfo();     // 加载编辑后的个人信息（昵称/头像）
    }

    /**
     * 从 SharedPreferences 加载编辑后的个人信息，立即刷新头像
     * 包括昵称和头像路径，编辑保存后自动同步到"我的"页面显示
     */
    private void loadProfileInfo() {
        long userId = SessionManager.getInstance(requireContext()).getUserId();
        SharedPreferences prefs = requireContext().getSharedPreferences("user_profile", Context.MODE_PRIVATE);

        // 刷新昵称
        String nickname = prefs.getString(userId + "_nickname", "");
        if (!nickname.isEmpty()) {
            tvNickname.setText(nickname);
        }

        // 刷新头像：跳过所有缓存，强制加载最新图片
        String avatarPath = prefs.getString(userId + "_avatar_path", "");
        if (!avatarPath.isEmpty()) {
            File avatarFile = new File(avatarPath);
            if (avatarFile.exists()) {
                // 关键：skipMemoryCache + 跳过磁盘缓存，确保每次都从文件重新加载
                Glide.with(this)
                        .load(avatarFile)
                        .skipMemoryCache(true)
                        .into(ivAvatar);
            }
        }

        // 刷新简介
        String bio = prefs.getString(userId + "_bio", "");
        if (!bio.isEmpty()) {
            tvBio.setText(bio);
        } else {
            tvBio.setText("这个人很懒，什么都没写");
        }
    }

    /**
     * 加载数据看板内容
     * 在后台线程查询数据库，统计总记录数、本周记录数、待同步记录数
     * 查询完成后通过 Handler 切回主线程更新 UI
     */
    private void loadDataPanel() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            long userId = SessionManager.getInstance(requireContext()).getUserId();
            List<MealRecord> userRecords = db.mealDao().getRecordsByUserId(userId);

            // 总记录数
            int total = userRecords.size();

            // 本周记录数（近7天）
            long weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
            int weekCount = 0;
            for (MealRecord r : userRecords) {
                if (r.getCreateTime() > weekAgo) weekCount++;
            }

            // 待同步记录数
            int unsynced = db.mealDao().getUnsyncedRecordsByUserId(userId).size();

            // 切回主线程更新 UI
            int finalTotal = total;
            int finalWeekCount = weekCount;
            int finalUnsynced = unsynced;
            new Handler(Looper.getMainLooper()).post(() -> {
                tvTotalRecords.setText(String.valueOf(finalTotal));
                tvWeekRecords.setText(String.valueOf(finalWeekCount));
                tvUnsyncedRecords.setText(String.valueOf(finalUnsynced));
            });
        }).start();
    }

    /**==================ContentProvider====================
     * 测试 ContentProvider 数据共享功能
     * 模拟外部应用通过 ContentResolver 查询本 App 的三餐记录
     * 查询成功则弹窗显示记录数量和内容
     */
    private void testContentProvider() {
        new Thread(() -> {
            android.database.Cursor cursor = requireContext().getContentResolver().query(
                    android.net.Uri.parse("content://com.example.mealdiary.provider/meal_record"),
                    null, null, null, null);

            StringBuilder sb = new StringBuilder();
            int count = 0;
            if (cursor != null) {
                count = cursor.getCount();
                while (cursor.moveToNext()) {
                    String mealType = cursor.getString(2);    // mealType
                    String foodName = cursor.getString(3);    // foodName
                    sb.append(mealType).append(": ").append(foodName).append("\n");
                }
                cursor.close();
            }

            String result = sb.length() > 0 ? sb.toString().trim() : "暂无记录";
            int finalCount = count;
            new Handler(Looper.getMainLooper()).post(() ->
                    new AlertDialog.Builder(requireContext())
                            .setTitle("ContentProvider 数据共享演示")
                            .setMessage("共查询到 " + finalCount + " 条记录：\n\n" + result
                                    + "\n\n↑ 以上数据通过 ContentProvider 获取")
                            .setPositiveButton("确定", null)
                            .show()
            );
        }).start();
    }
}