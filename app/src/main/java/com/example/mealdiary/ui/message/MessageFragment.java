package com.example.mealdiary.ui.message;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.mealdiary.R;
import com.example.mealdiary.data.local.database.AppDatabase;
import com.example.mealdiary.data.local.entity.MealRecord;
import com.example.mealdiary.data.local.SessionManager;
import com.example.mealdiary.service.MealReminderReceiver;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

/**
 * 饮食健康中心 Fragment
 *
 * 功能说明：
 * 1. 连续打卡天数 — 计算用户从今天往前连续记录饮食的天数，激励用户坚持记录
 * 2. 近期解锁食物 — 展示用户最近记录的5种不重复食物名称，回顾饮食多样性
 * 3. 本周饮食分析 — 融合"趋势统计"和"漏餐分析"：
 *    - 趋势统计：最近7天早餐/午餐/晚餐各吃了多少次
 *    - 漏餐分析：最近7天各漏了多少天，给出针对性提醒
 * 4. 三餐提醒 — 用户可自定义早/午/晚餐的提醒时间，到点弹通知
 */
public class MessageFragment extends Fragment {

    // ===== UI控件 =====
    private TextView tvStreak;                    // 连续打卡天数显示
    private TextView tvRecentFoods;               // 近期解锁食物显示
    private TextView tvMissedMeals;               // 本周饮食分析显示
    private Button btnReminderBreakfast;          // 早餐提醒按钮
    private Button btnReminderLunch;              // 午餐提醒按钮
    private Button btnReminderDinner;             // 晚餐提醒按钮

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);

        // ===== 绑定控件 =====
        tvStreak = view.findViewById(R.id.tvStreak);
        tvRecentFoods = view.findViewById(R.id.tvRecentFoods);
        tvMissedMeals = view.findViewById(R.id.tvMissedMeals);
        btnReminderBreakfast = view.findViewById(R.id.btnReminderBreakfast);
        btnReminderLunch = view.findViewById(R.id.btnReminderLunch);
        btnReminderDinner = view.findViewById(R.id.btnReminderDinner);

        // ===== 三餐提醒按钮点击事件 =====
        // 点击后弹出时间选择器，让用户自定义提醒时间
        // 默认时间：早餐8:00、午餐12:00、晚餐18:00
        btnReminderBreakfast.setOnClickListener(v -> showTimePicker("breakfast", 8, 0));
        btnReminderLunch.setOnClickListener(v -> showTimePicker("lunch", 12, 0));
        btnReminderDinner.setOnClickListener(v -> showTimePicker("dinner", 18, 0));

        return view;
    }

    /**
     * 页面恢复时重新加载健康数据
     * 确保从其他页面返回时数据是最新的
     */
    @Override
    public void onResume() {
        super.onResume();
        loadHealthData();
    }

    /**
     * 加载健康中心所有数据
     *
     * 在后台线程执行数据库查询，避免阻塞主线程
     * 包含：打卡天数、解锁食物、本周分析
     * 查询完成后通过 Handler 切回主线程更新 UI
     */
    private void loadHealthData() {
        new Thread(() -> {
            // 获取当前用户的数据库实例
            AppDatabase db = AppDatabase.getInstance(requireContext());
            long userId = SessionManager.getInstance(requireContext()).getUserId();
            // 获取当前用户的所有饮食记录
            List<MealRecord> allRecords = db.mealDao().getRecordsByUserId(userId);

            // 在后台线程计算各项数据
            int streak = calculateStreak(allRecords);               // 计算连续打卡天数
            String recentFoods = getRecentFoods(allRecords);        // 获取近期解锁食物
            String weekAnalysis = getWeekAnalysis(allRecords);      // 获取本周饮食分析

            // 切回主线程更新 UI
            new Handler(Looper.getMainLooper()).post(() -> {
                tvStreak.setText("连续记录 " + streak + " 天");
                tvRecentFoods.setText(recentFoods);
                tvMissedMeals.setText(weekAnalysis);
            });
        }).start();
    }

    /**
     * 获取近期解锁食物
     *
     * 从所有记录中提取最近5种不重复的食物名称
     * 使用 HashSet 去重，确保同一种食物只显示一次
     *
     * @param records 用户的所有饮食记录（按时间倒序）
     * @return 格式如 "米饭 · 面条 · 饺子 · 火锅 · 沙拉"，无记录则返回 "暂无记录"
     */
    private String getRecentFoods(List<MealRecord> records) {
        if (records.isEmpty()) return "暂无记录";

        StringBuilder sb = new StringBuilder();
        HashSet<String> seen = new HashSet<>();  // 用于去重，记录已经出现过的食物名
        int count = 0;

        for (MealRecord r : records) {
            if (count >= 5) break;  // 最多取5种不重复食物

            String name = r.getFoodName();
            // 如果食物名不为空且没出现过，则加入结果
            if (name != null && !seen.contains(name)) {
                seen.add(name);
                if (sb.length() > 0) sb.append(" · ");  // 用 · 分隔多个食物
                sb.append(name);
                count++;
            }
        }
        return sb.toString();
    }

    /**
     * 本周饮食分析（融合趋势统计与漏餐分析）
     *
     * 统计最近7天（从今天往前推7天）的饮食数据：
     * - 趋势统计：早餐/午餐/晚餐各吃了多少次
     * - 漏餐分析：各餐漏了多少天
     *
     * 遍历最近7天，每天检查是否有早/午/晚餐的记录，
     * 没有则计入漏餐次数
     *
     * @param records 用户的所有饮食记录
     * @return 格式如：
     *         "本周趋势：🥐12 🍱10 🍽️8
     *          • 漏掉早餐 3 天
     *          • 漏掉晚餐 1 天"
     *         或 "🎉 三餐规律，非常健康！"
     */
    private String getWeekAnalysis(List<MealRecord> records) {
        // 获取今天0点的时间，作为统计的起点
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        int totalBreakfast = 0, totalLunch = 0, totalDinner = 0;   // 各餐总次数
        int missedBreakfast = 0, missedLunch = 0, missedDinner = 0; // 各餐漏掉天数

        // 遍历最近7天（i=0是今天，i=1是昨天，...，i=6是7天前）
        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) today.clone();
            day.add(Calendar.DAY_OF_MONTH, -i);

            // 这一天的开始时间戳和结束时间戳
            long dayStart = day.getTimeInMillis();
            long dayEnd = dayStart + 24 * 60 * 60 * 1000L;

            // 检查这一天是否有各餐的记录
            boolean hasBreakfast = false, hasLunch = false, hasDinner = false;
            for (MealRecord r : records) {
                if (r.getCreateTime() >= dayStart && r.getCreateTime() < dayEnd) {
                    String type = r.getMealType();
                    if ("breakfast".equals(type)) { hasBreakfast = true; totalBreakfast++; }
                    else if ("lunch".equals(type)) { hasLunch = true; totalLunch++; }
                    else if ("dinner".equals(type)) { hasDinner = true; totalDinner++; }
                }
            }

            // 没有对应餐的记录，则漏餐天数+1
            if (!hasBreakfast) missedBreakfast++;
            if (!hasLunch) missedLunch++;
            if (!hasDinner) missedDinner++;
        }

        // 构建分析结果文字
        StringBuilder sb = new StringBuilder();
        // 先展示趋势统计
        sb.append("本周趋势：🥐").append(totalBreakfast)
                .append(" 🍱").append(totalLunch)
                .append(" 🍽️").append(totalDinner).append("\n");

        // 再展示漏餐分析
        if (missedBreakfast > 0) sb.append("• 漏掉早餐 ").append(missedBreakfast).append(" 天\n");
        if (missedLunch > 0) sb.append("• 漏掉午餐 ").append(missedLunch).append(" 天\n");
        if (missedDinner > 0) sb.append("• 漏掉晚餐 ").append(missedDinner).append(" 天\n");

        // 如果三餐都没漏，给正面反馈
        if (missedBreakfast == 0 && missedLunch == 0 && missedDinner == 0) {
            sb.append("🎉 三餐规律，非常健康！");
        }
        return sb.toString();
    }

    /**
     * 计算连续打卡天数
     *
     * 从今天开始往前推，逐天检查是否有任何记录
     * 直到遇到没有记录的那一天停止
     *
     * @param records 用户的所有饮食记录
     * @return 连续打卡天数（今天有记录至少算1天，没有任何记录返回0）
     */
    private int calculateStreak(List<MealRecord> records) {
        if (records.isEmpty()) return 0;

        // 获取今天0点
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        int streak = 0;
        Calendar checkDay = (Calendar) today.clone();

        // 从今天开始逐天往前检查
        while (true) {
            long dayStart = checkDay.getTimeInMillis();
            long dayEnd = dayStart + 24 * 60 * 60 * 1000L;

            // 检查这一天是否有任何记录
            boolean hasRecord = false;
            for (MealRecord r : records) {
                if (r.getCreateTime() >= dayStart && r.getCreateTime() < dayEnd) {
                    hasRecord = true;
                    break;  // 找到一条就够了
                }
            }

            if (hasRecord) {
                streak++;  // 有记录，打卡天数+1
                checkDay.add(Calendar.DAY_OF_MONTH, -1);  // 往前推一天继续检查
            } else {
                break;  // 没记录了，停止计数
            }
        }
        return streak;
    }

    /**
     * 弹出时间选择器，让用户自定义提醒时间
     *
     * 选择后自动保存到 SharedPreferences，下次打开记住上次设置的时间
     * 然后调用 setReminder() 设置系统闹钟
     *
     * @param mealType      餐类标识（breakfast/lunch/dinner）
     * @param defaultHour   默认小时（24小时制）
     * @param defaultMinute 默认分钟
     */
    private void showTimePicker(String mealType, int defaultHour, int defaultMinute) {
        // 读取用户之前保存的时间，没有则用默认值
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("meal_reminders", Context.MODE_PRIVATE);
        int savedHour = prefs.getInt(mealType + "_hour", defaultHour);
        int savedMinute = prefs.getInt(mealType + "_minute", defaultMinute);

        // 弹出 Android 系统时间选择器
        TimePickerDialog timePicker = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    // 用户选好时间后，保存到 SharedPreferences
                    prefs.edit()
                            .putInt(mealType + "_hour", hourOfDay)
                            .putInt(mealType + "_minute", minute)
                            .apply();

                    // 获取中文餐类名称
                    String mealName = mealType.equals("breakfast") ? "早餐" :
                            mealType.equals("lunch") ? "午餐" : "晚餐";

                    // 设置系统闹钟
                    setReminder(hourOfDay, minute, mealName);
                }, savedHour, savedMinute, true);  // true 表示使用24小时制
        timePicker.show();
    }

    /**
     * 调用系统 AlarmManager 设置每日重复闹钟
     *
     * 闹钟触发时，系统会发送广播给 MealReminderReceiver，
     * MealReminderReceiver 收到广播后发送通知提醒用户用餐
     *
     * @param hour     提醒小时（24小时制）
     * @param minute   提醒分钟
     * @param mealName 餐类名称（早餐/午餐/晚餐），会显示在通知内容中
     */
    private void setReminder(int hour, int minute, String mealName) {
        AlarmManager alarmManager = (AlarmManager) requireContext()
                .getSystemService(Context.ALARM_SERVICE);

        // 创建 Intent，指向 MealReminderReceiver（接收闹钟广播的Receiver）
        Intent intent = new Intent(requireContext(), MealReminderReceiver.class);
        intent.putExtra("meal_name", mealName);  // 传递餐类名称，用于通知内容

        // 使用 mealName 的 hashCode 作为 requestCode，确保三个提醒不会互相覆盖
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                mealName.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 设置闹钟触发时间
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // 如果设置的时间已经过了（比当前时间早），改为明天同一时间
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        // 设置每日重复闹钟
        // RTC_WAKEUP：使用系统时间，会唤醒设备
        // INTERVAL_DAY：间隔为一天
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent);

        // 提示用户设置成功
        Toast.makeText(getContext(),
                mealName + "提醒已设置（每天 " + hour + ":" + String.format("%02d", minute) + "）",
                Toast.LENGTH_SHORT).show();
    }
}