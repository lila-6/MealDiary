package com.example.mealdiary.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.example.mealdiary.R;
import com.example.mealdiary.data.local.database.AppDatabase;
import com.example.mealdiary.data.local.entity.MealRecord;
import com.example.mealdiary.data.local.SessionManager;
import com.example.mealdiary.ui.diary.AddMealActivity;
import com.example.mealdiary.ui.shake.ShakePickActivity;
import java.util.Calendar;
import java.util.List;

/**
 * 首页 Fragment（ViewPager2 的 Tab 0）
 *
 * 功能：
 * 1. 今日统计 — 显示今日已记录餐数和历史总记录数
 * 2. 摇一摇入口 — 点击卡片跳转 ShakePickActivity，随机选餐
 * 3. 快捷入口 — "记录一餐"跳转添加页，"饮食日记"发送广播切换 Tab
 *
 * 数据来源：Room 数据库，按当前登录用户 ID 过滤，确保数据隔离
 */
public class HomeFragment extends Fragment {

    // ===== UI控件 =====
    private TextView tvTodayCount;  // 今日已记录餐数
    private TextView tvTotalCount;  // 历史总记录数

    //1. 先走：创建布局，返回 View 对象
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false); //把 XML 变成 Java 的 View 对象
    }

    //// 2. 再走：布局已存在，可以安全地 find 控件
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ===== 绑定控件 =====
        tvTodayCount = view.findViewById(R.id.tvTodayCount);
        tvTotalCount = view.findViewById(R.id.tvTotalCount);
        CardView cardAddMeal = view.findViewById(R.id.cardAddMeal);          // "记录一餐"卡片
        CardView cardViewDiary = view.findViewById(R.id.cardViewDiary);      // "饮食日记"卡片
        CardView cardShakePick = view.findViewById(R.id.cardShakePick);      // "今天吃什么"摇一摇卡片

        // ===== 快捷入口："记录一餐" → 跳转 AddMealActivity =====
        cardAddMeal.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), AddMealActivity.class)));

        // ===== 快捷入口："饮食日记" → 发广播切换 Tab =====
        // 因为 HomeFragment 无法直接操作 ViewPager2，通过发送自定义广播
        // 让 MainActivity 中的 Receiver 收到后切换到饮食日记页（Tab 1）
        cardViewDiary.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().sendBroadcast(new Intent("SWITCH_TO_DIARY"));
            }
        });

        // ===== 摇一摇入口 → 跳转 ShakePickActivity =====
        cardShakePick.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ShakePickActivity.class)));
    }

    /**
     * 页面恢复时重新加载统计数据
     * 确保从其他页面返回时数据是最新的
     */
    @Override
    public void onResume() {
        super.onResume();
        loadTodayStats();
    }

    /**
     * 加载今日统计数据
     *
     * 在后台线程查询数据库：
     * - 今日已记录餐数：统计今天0点到现在的记录数
     * - 历史总记录数：统计该用户所有记录数
     *
     * 查询完成后通过 Handler 切回主线程更新 UI
     */
    private void loadTodayStats() {
        new Thread(() -> {
            // 获取数据库实例和当前用户ID
            AppDatabase db = AppDatabase.getInstance(requireContext());
            long userId = SessionManager.getInstance(requireContext()).getUserId();

            // 计算今天0点的时间戳（毫秒）
            Calendar todayStart = Calendar.getInstance();
            todayStart.set(Calendar.HOUR_OF_DAY, 0);
            todayStart.set(Calendar.MINUTE, 0);
            todayStart.set(Calendar.SECOND, 0);
            todayStart.set(Calendar.MILLISECOND, 0);
            long todayStartTime = todayStart.getTimeInMillis();

            // 获取当前用户的所有记录
            List<MealRecord> userRecords = db.mealDao().getRecordsByUserId(userId);

            // 统计今日记录数：遍历所有记录，筛选出今天创建的
            int todayCount = 0;
            for (MealRecord r : userRecords) {
                if (r.getCreateTime() >= todayStartTime) todayCount++;
            }
            // 总记录数就是该用户所有记录的数量
            int totalCount = userRecords.size();

            // 切回主线程更新 UI
            int finalTodayCount = todayCount;
            int finalTotalCount = totalCount;
            new Handler(Looper.getMainLooper()).post(() -> {
                tvTodayCount.setText(String.valueOf(finalTodayCount));
                tvTotalCount.setText(String.valueOf(finalTotalCount));
            });
        }).start();
    }
}