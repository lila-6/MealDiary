package com.example.mealdiary.ui.diary;

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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.applandeo.materialcalendarview.CalendarView;
import com.example.mealdiary.R;
import com.example.mealdiary.data.local.database.AppDatabase;
import com.example.mealdiary.data.local.entity.MealRecord;
import com.example.mealdiary.data.local.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 饮食日记 Fragment（ViewPager2 的 Tab 1）
 *
 * 功能：
 * 1. 日历视图 — 使用 Applandeo CalendarView 库，按月显示日历
 * 2. 当日记录列表 — 点击日历某一天，展示该天的所有饮食记录
 * 3. 添加记录入口 — FAB 按钮，传递选中日期给 AddMealActivity，支持补录历史
 *
 * 日历用的是 userId + 日期 双重过滤。
 *
 * 数据来源：Room 数据库，按当前用户ID和选中日期过滤
 * 日历库：com.applandeo:material-calendar-view（替换了无法适配鸿蒙的旧库）
 */
public class DiaryFragment extends Fragment {

    // ===== UI控件 =====
    private RecyclerView recyclerView;           // 饮食记录列表
    private FloatingActionButton fabAdd;         // 添加记录按钮（FAB）
    private MealAdapter adapter;                 // 列表适配器
    private CalendarView calendarView;           // 日历控件（Applandeo库）
    private TextView tvDiaryDateTitle;           // 当前选中日期标题（"今天"或具体日期）

    // ===== 数据字段 =====
    private String currentDate;                  // 当前选中日期，格式 yyyy-MM-dd
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());  // 日期格式化工具

    ////1. 先走：创建布局，返回 View 对象
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary, container, false);

        // ===== 绑定控件 =====
        recyclerView = view.findViewById(R.id.recyclerView);
        fabAdd = view.findViewById(R.id.fabAdd);
        calendarView = view.findViewById(R.id.calendarView);
        tvDiaryDateTitle = view.findViewById(R.id.tvDiaryDateTitle);

        // ===== 设置 RecyclerView .设置饮食记录列表显示=====
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        /// 建适配器，传入空列表 + 点击回调（点击后跳转详情页）
        adapter = new MealAdapter(new ArrayList<>(), record -> {
            // 点击列表项 → 跳转到详情页
            Intent intent = new Intent(getActivity(), DetailActivity.class);
            intent.putExtra("recordId", record.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // ===== FAB 点击 → 跳转添加记录页 =====
        // 将当前选中的日期通过 Intent 传递给 AddMealActivity
        // 这样用户在历史日期点+号，就能在那一天补录记录
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddMealActivity.class);
            intent.putExtra("targetDate", currentDate);  // 传递选中日期
            startActivity(intent);
        });

        // ===== 默认选中今天 =====
        Calendar today = Calendar.getInstance();
        currentDate = sdf.format(today.getTime());
        tvDiaryDateTitle.setText("今天");

        // ===== 日历点击事件 =====
        // 用户点击日历上某一天 → 更新 currentDate → 加载当天记录
        calendarView.setOnDayClickListener(eventDay -> {
            Calendar cal = eventDay.getCalendar();
            currentDate = sdf.format(cal.getTime());
            // 判断是否是今天，标题显示"今天"或具体日期
            if (isToday(cal)) {
                tvDiaryDateTitle.setText("今天");
            } else {
                tvDiaryDateTitle.setText(currentDate);
            }
            loadRecords();  // 加载选中日期的记录
        });

        return view;
    }

    /**
     * 页面恢复时刷新记录列表
     * 确保从添加页返回后能看到新添加的记录
     */
    @Override
    public void onResume() {
        super.onResume();
        loadRecords();
    }

    /**
     * 加载当前选中日期的饮食记录
     *
     * 在后台线程查询 Room 数据库，按用户ID和日期过滤
     * 查询完成后通过 Handler 切回主线程更新 RecyclerView
     */
    private void loadRecords() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            long userId = SessionManager.getInstance(requireContext()).getUserId();
            // 按用户ID和日期查询：MealDao.getRecordsByDate(userId, currentDate)
            List<MealRecord> records = db.mealDao().getRecordsByDate(userId, currentDate);
            // 切回主线程刷新列表
            new Handler(Looper.getMainLooper()).post(() -> adapter.setRecords(records));
        }).start();
    }

    /**
     * 判断给定的 Calendar 是否是今天
     *
     * @param cal 要判断的日期
     * @return true=今天，false=不是今天
     */
    private boolean isToday(Calendar cal) {
        Calendar today = Calendar.getInstance();
        // 比较年份和一年中的第几天
        return cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }
}