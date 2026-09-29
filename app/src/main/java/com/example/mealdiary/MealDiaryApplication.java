package com.example.mealdiary;

import android.app.Application;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.util.Log;
import com.example.mealdiary.service.SyncService;

/**
 * 全局 Application 类
 * 作用：在 App 整个生命周期内监听网络变化，网络恢复时自动触发 SyncService
 */
public class MealDiaryApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        registerNetworkCallback();
    }

    private void registerNetworkCallback() {
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d("SyncService", "网络恢复，触发同步");
                Intent i = new Intent(MealDiaryApplication.this, SyncService.class);
                startForegroundService(i);
            }
        });
    }
}