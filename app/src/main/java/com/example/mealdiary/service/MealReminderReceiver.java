package com.example.mealdiary.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.mealdiary.ui.MainActivity;

/**
 * 三餐提醒广播接收器
 *
 * 当系统闹钟（AlarmManager）触发时，此 Receiver 被调用
 * AlarmManager 是在 MessageFragment.java 里的 setReminder() 方法中调用的。
 *
 * 工作流程：
 * 1. MessageFragment 中用户设置提醒时间 → setReminder() 调用 setRepeating 设置每日重复闹钟
 * 2. 系统到时间触发闹钟 → 发送广播 → 此 Receiver 的 onReceive 被调用
 * 3. onReceive 中发送通知提醒用户用餐
 * 4. setRepeating 会自动每天重复，无需在此处重新设置闹钟
 */
public class MealReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // 从 Intent 中取出餐类名称，用于通知内容
        String mealName = intent.getStringExtra("meal_name");
        if (mealName == null) mealName = "用餐";

        // ===== 发送通知 =====
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8.0+ 必须创建通知渠道，否则通知发不出去
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "meal_reminder",                      // 渠道ID
                    "用餐提醒",                          // 渠道名称（用户可在系统设置中看到）
                    NotificationManager.IMPORTANCE_HIGH); // 高优先级，会响铃和震动
            channel.setDescription("提醒按时用餐");
            manager.createNotificationChannel(channel);
        }

        // 点击通知后跳转到 MainActivity
        Intent clickIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 构建通知内容
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "meal_reminder")
                .setSmallIcon(android.R.drawable.ic_dialog_info)  // 通知栏小图标
                .setContentTitle("🍽️ 用餐提醒")
                .setContentText("该吃" + mealName + "啦！记得记录哦~")
                .setPriority(NotificationCompat.PRIORITY_HIGH)    // 高优先级
                .setContentIntent(pendingIntent)                  // 点击跳转
                .setAutoCancel(true);                             // 点击后自动消失

        // 发送通知，用 mealName 的 hashCode 作为通知 ID，避免三个提醒互相覆盖
        manager.notify(mealName.hashCode(), builder.build());
    }
}