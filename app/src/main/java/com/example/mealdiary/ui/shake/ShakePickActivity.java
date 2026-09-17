package com.example.mealdiary.ui.shake;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mealdiary.R;
import com.example.mealdiary.data.local.database.AppDatabase;
import com.example.mealdiary.data.local.entity.MealRecord;
import com.example.mealdiary.data.local.SessionManager;
import com.example.mealdiary.ui.diary.DetailActivity;
import java.util.List;
import java.util.Random;

/**
 * 摇一摇随机选餐 Activity（多种组件之：传感器组件）
 *
 * 功能：用户摇晃手机后，从历史饮食记录中随机选取一条，
 * 弹窗展示食物信息，用户可选择"就去记录"或"再摇一次"
 *
 * 传感器使用：加速度传感器（Sensor.TYPE_ACCELEROMETER）
 * - 检测手机在三个轴（X/Y/Z）上的加速度变化
 * - 当加速度超过阈值（12 m/s²）时判定为"摇动"
 * - 防抖处理：0.8秒内不重复触发
 *
 * 实现 SensorEventListener 接口：
 * - onSensorChanged()：传感器数据变化时回调
 * - onAccuracyChanged()：传感器精度变化时回调
 */
public class ShakePickActivity extends AppCompatActivity implements SensorEventListener {

    // ===== 传感器相关 =====
    private SensorManager sensorManager;    // 传感器管理器，用于获取传感器和注册监听
    private Sensor accelerometer;           // 加速度传感器
    private long lastShakeTime;             // 上次摇动的时间戳，用于防抖

    // ===== UI控件 =====
    private TextView tvShakeHint;           // 摇一摇提示文字

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shake_pick);

        // 显示提示文字
        tvShakeHint = findViewById(R.id.tvShakeHint);
        tvShakeHint.setText("摇一摇\n随机选一餐");

        // 获取传感器管理器
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            // 获取加速度传感器
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    /**
     * 页面可见时注册传感器监听
     * 使用 SENSOR_DELAY_UI 灵敏度，适合界面交互
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null && sensorManager != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            //registerListener 注册传感器
        }
    }

    /**
     * 页面不可见时注销传感器监听，节省电量
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            //unregisterListener 注销传感器
        }
    }

    /**
     * 传感器数据变化回调
     *
     * 检测逻辑：
     * 1. 计算三个轴的合加速度，减去重力加速度得到实际运动加速度
     * 2. 如果加速度超过阈值（12），判定为一次摇动
     * 3. 防抖：距离上次摇动不足 0.8 秒则忽略
     *
     * @param event 传感器事件，values[0]=X轴, values[1]=Y轴, values[2]=Z轴
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // 获取三个轴的加速度值
        float x = event.values[0];  // X轴（左右）
        float y = event.values[1];  // Y轴（上下）
        float z = event.values[2];  // Z轴（前后）

        // 计算实际运动加速度（合加速度 - 重力加速度）
        double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

        // 如果加速度超过阈值（12 m/s²），判定为摇动
        if (acceleration > 12) {
            long now = System.currentTimeMillis();
            // 防抖：距离上次摇动不足 0.8 秒则忽略
            if (now - lastShakeTime > 800) {
                lastShakeTime = now;
                performShake();  // 执行随机选餐
            }
        }
    }

    /**
     * 执行摇一摇后的随机选餐逻辑
     *
     * 在后台线程查询数据库，随机选取一条记录：
     * 1. 获取当前用户的所有饮食记录
     * 2. 如果没有任何记录，提示用户先去记录
     * 3. 随机选一条，跳转到详情页展示
     * 4. Toast 显示选中的食物名称
     */
    private void performShake() {
        new Thread(() -> {
            // 获取数据库实例和当前用户ID
            AppDatabase db = AppDatabase.getInstance(this);
            long userId = SessionManager.getInstance(this).getUserId();

            // 查询当前用户的所有饮食记录
            List<MealRecord> allRecords = db.mealDao().getRecordsByUserId(userId);

            // 如果没有记录，提示用户
            if (allRecords.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(this, "还没有记录，先去记录一餐吧", Toast.LENGTH_SHORT).show());
                return;
            }

            // 随机选取一条记录
            MealRecord randomRecord = allRecords.get(new Random().nextInt(allRecords.size()));

            // 切回主线程：跳转到详情页展示
            new Handler(Looper.getMainLooper()).post(() -> {
                Intent intent = new Intent(this, DetailActivity.class);
                intent.putExtra("recordId", randomRecord.getId());  // 传递记录ID
                startActivity(intent);
                Toast.makeText(this, "摇到: " + randomRecord.getFoodName(), Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    /**
     * 将餐类英文标识转换为中文显示
     * @param type 餐类标识（breakfast/lunch/dinner/snack）
     * @return 中文餐类名称
     */
    private String getMealTypeName(String type) {
        switch (type != null ? type : "") {
            case "breakfast": return "早餐";
            case "lunch":     return "午餐";
            case "dinner":    return "晚餐";
            case "snack":     return "加餐";
            default:          return "餐食";
        }
    }

    /**
     * 传感器精度变化回调（此处无需处理）
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}