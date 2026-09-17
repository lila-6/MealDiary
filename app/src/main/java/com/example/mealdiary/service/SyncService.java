package com.example.mealdiary.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.mealdiary.data.local.database.AppDatabase;
import com.example.mealdiary.data.local.entity.MealRecord;
import com.example.mealdiary.data.remote.ApiService;
import com.example.mealdiary.data.remote.RetrofitClient;
import com.example.mealdiary.ui.MainActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 后台数据同步服务（四大组件之一：Service）
 *
 * 作用：在后台将本地未同步的饮食记录上传到若依后端
 * 把本地 Room 里"未同步"的记录上传到若依后端。
 * 显式 Intent + start 启动，没有用绑定。
 *Service + BroadcastReceiver + Retrofit 联动的核心场景
 *
 * 实际启动来源（共2个）：
 * 1. MainActivity 启动时手动触发一次（onCreate 中）
 * 2. MainActivity 中动态注册的 Receiver 检测到网络变化时启动
 *
 * 注意：清单文件中静态注册的 NetworkChangeReceiver 因 Android 7.0+ 系统限制，
 * 无法接收 CONNECTIVITY_CHANGE 广播，实际网络监听由 MainActivity 动态注册完成。
 *
 * 把 Room 里的数据取出来，封装成 Map（自动转 JSON），然后通过 Retrofit POST 到若依后端。
 *
 * 工作流程：
 * 1. 被启动 → 发送前台通知，告知用户"正在同步"
 * 2. 检查网络是否可用 → 不可用则直接停止
 * 3. 后台线程查询 Room 中 syncStatus=0 的记录
 * 4. 逐条通过 Retrofit POST 到若依 /diet/record/add
 * 5. 上传成功 → syncStatus 改为 1 → 更新 Room
 * 6. 全部完成 → stopSelf() 停止服务
 *
 * 为什么是前台服务：
 * - Android 8.0+ 限制后台 Service，必须使用前台服务
 * - 前台服务需要在通知栏显示常驻通知
 * - foregroundServiceType="dataSync" 声明为数据同步类型
 */

public class SyncService extends Service {

    // 通知渠道ID，用于 Android 8.0+ 的通知分类
    private static final String CHANNEL_ID = "sync_channel";
    // 通知ID，用于标识前台服务的通知
    private static final int NOTIFICATION_ID = 1;

    /**
     * Service 创建时初始化通知渠道
     * 只调用一次
     */
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    /**
     * Service 每次启动时调用（通过 startService / startForegroundService）
     *
     * 执行步骤：
     * 1. 创建前台通知，让系统知道这是一个前台服务
     * 2. 检查网络状态，不可用则跳过本次同步
     * 3. 后台线程查询未同步记录并逐条上传
     * 4. 同步完成后调用 stopSelf() 停止自己
     *
     * @param intent  启动时传入的 Intent
     * @param flags   启动标志
     * @param startId 启动ID
     * @return START_NOT_STICKY：被杀后不自动重启
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // ===== 创建前台通知 =====
        // 点击通知后跳转回 MainActivity
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 构建通知内容
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("三餐打卡同步中")              // 通知标题
                .setContentText("正在同步饮食数据...")          // 通知内容
                .setSmallIcon(android.R.drawable.ic_popup_sync) // 通知栏小图标
                .setContentIntent(pendingIntent)                 // 点击跳转
                .setOngoing(true);                               // 常驻通知，不能滑动删除

        // 启动前台服务，通知栏显示通知
        startForeground(NOTIFICATION_ID, builder.build());

        // ===== 检查网络是否可用 =====
        // 如果当前没有网络，直接停止服务，不做无用功
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnected()) {
            Log.d("SyncService", "网络不可用，跳过同步");
            stopSelf();  // 停止自己
            return START_NOT_STICKY;
        }

        // ===== 后台线程执行同步 =====
        // Room 禁止主线程操作数据库，所以用 new Thread 切到后台
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(SyncService.this);

            // 查询所有未同步的记录（syncStatus=0）
            List<MealRecord> unsynced = db.mealDao().getUnsyncedRecords();

            if (!unsynced.isEmpty()) {
                try {
                    // 获取 Retrofit API 接口
                    ApiService api = RetrofitClient.getApiService();

                    // 逐条上传未同步记录
                    for (MealRecord r : unsynced) {
                        // 把 MealRecord 对象的字段封装成 Map
                        Map<String, Object> recordMap = new java.util.HashMap<>();
                        recordMap.put("userId", r.getUserId());
                        recordMap.put("mealType", r.getMealType());
                        recordMap.put("foodName", r.getFoodName());
                        recordMap.put("imagePath", r.getImagePath());
                        recordMap.put("audioPath", r.getAudioPath());
                        recordMap.put("note", r.getNote());
                        // 将时间戳转换为日期字符串，匹配若依后端的 DATETIME 格式
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        recordMap.put("createTime", sdf.format(new Date(r.getCreateTime())));
                        recordMap.put("syncStatus", r.getSyncStatus());

                        // 调用若依后端的添加接口 POST /diet/record/add
                        retrofit2.Response<Map<String, Object>> response = api.uploadRecord(recordMap).execute();
                        Log.d("SyncService", "服务器返回：" + response.raw().toString());

                        // 上传成功 → 标记为已同步
                        if (response.isSuccessful()) {
                            r.setSyncStatus(1);
                            db.mealDao().update(r);  // 更新 Room 中的记录
                        }
                    }
                    Log.d("SyncService", "同步完成：" + unsynced.size() + "条");
                } catch (Exception e) {
                    Log.e("SyncService", "同步失败", e);
                }
            }

            // 同步完成（或没有需要同步的数据），停止服务
            stopSelf();
        }).start();

        // START_NOT_STICKY：如果 Service 被系统杀掉，不自动重启
        return START_NOT_STICKY;
    }

    /**
     * 此 Service 不需要绑定，返回 null
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 创建通知渠道
     * Android 8.0（API 26）及以上必须创建通知渠道，否则通知发不出去
     * 渠道ID为 "sync_channel"，用户可在系统设置中看到并管理
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,                       // 渠道ID
                    "数据同步",                       // 渠道名称（用户可见）
                    NotificationManager.IMPORTANCE_LOW // 低优先级，不响铃不震动
            );
            channel.setDescription("三餐数据后台同步通知");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}