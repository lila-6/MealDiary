package com.example.mealdiary.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * 网络变化监听广播接收器（四大组件之一：BroadcastReceiver）
 *
 * 注册方式：在 AndroidManifest.xml 中静态注册，声明监听 CONNECTIVITY_CHANGE 广播
 *          注意：Android 7.0（API 24）起，系统限制此广播只能由动态注册的 Receiver 接收，
 *          静态注册仅作为代码示范，展示 BroadcastReceiver 的标准声明方式。
 *          实际网络监听由 MainActivity 中动态注册的 Receiver 完成。
 *
 * 工作流程：
 * 1. 手机网络状态发生变化（WiFi连接/断开、移动数据连接/断开）
 * 2. 系统发送 CONNECTIVITY_CHANGE 广播
 * 3. 此 Receiver 的 onReceive() 被调用（低版本系统）或被忽略（Android 7.0+）
 * 4. 检查当前网络是否可用
 *    - 可用 → 启动 SyncService 进行数据同步
 *    - 不可用 → 不做任何操作
 *
 * 双重注册说明：
 * - 静态注册（本类）：清单文件中声明，展示标准用法，受系统版本限制
 * - 动态注册（MainActivity 中）：运行时注册，绕过 Android 7.0+ 限制，实际负责触发同步
 * 两种方式互补，全面展示 BroadcastReceiver 的注册机制
 */
public class NetworkChangeReceiver extends BroadcastReceiver {

    /**
     * 收到网络变化广播时的回调方法
     * @param context 上下文，用于获取系统服务和启动 Service
     * @param intent  广播携带的 Intent，可获取额外数据（此处未使用）
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // 获取系统连接管理服务
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            // 获取当前活跃的网络信息
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            // 判断网络是否已连接（WiFi 或移动数据）
            if (activeNetwork != null && activeNetwork.isConnected()) {
                // 网络可用 → 启动 SyncService 进行数据同步
                Intent syncIntent = new Intent(context, SyncService.class);
                // Android 8.0+ 必须使用 startForegroundService 启动前台服务
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(syncIntent);
                } else {
                    context.startService(syncIntent);
                }
            }
        }
    }
}