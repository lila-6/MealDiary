package com.example.mealdiary.ui.diary;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.mealdiary.R;
import com.example.mealdiary.data.local.database.AppDatabase;
import com.example.mealdiary.data.local.entity.MealRecord;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 饮食记录详情页 Activity
 *
 * 功能：
 * 1. 查看详情 — 显示食物名称、餐类、记录时间、备注
 * 2. 加载图片 — 使用 Glide 从本地路径加载饮食照片
 * 3. 播放录音 — 使用 MediaPlayer 播放录制好的音频（多种组件之：多媒体）
 * 4. 删除记录 — 弹窗确认后从 Room 数据库删除，不可恢复
 *
 * 数据来源：通过 Intent 接收 recordId，从 Room 数据库查询对应的 MealRecord
 * 图片加载：Glide 库，支持文件路径加载和占位图
 */
public class DetailActivity extends AppCompatActivity {

    // ===== UI控件 =====
    private TextView tvFoodName;     // 食物名称
    private TextView tvMealType;     // 餐类（早餐/午餐/晚餐/加餐）
    private TextView tvTime;         // 记录时间
    private TextView tvNote;         // 文字备注
    private ImageView ivPhoto;       // 饮食照片
    private Button btnPlayAudio;     // 播放录音按钮
    private Button btnDelete;        // 删除记录按钮

    // ===== 多媒体 =====
    private MediaPlayer mediaPlayer; // 音频播放器（多种组件之：多媒体组件）

    // 后台线程池，用于数据库操作（Room 禁止主线程访问）
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // ===== 绑定控件 =====
        tvFoodName = findViewById(R.id.tvFoodName);
        tvMealType = findViewById(R.id.tvMealType);
        tvTime = findViewById(R.id.tvTime);
        tvNote = findViewById(R.id.tvNote);
        ivPhoto = findViewById(R.id.ivPhoto);
        btnPlayAudio = findViewById(R.id.btnPlayAudio);
        btnDelete = findViewById(R.id.btnDelete);

        // ===== 获取传递过来的记录ID =====
        // recordId 由上一个页面（DiaryFragment/HomeFragment/ShakePickActivity）通过 Intent 传递
        long recordId = getIntent().getLongExtra("recordId", -1);
        if (recordId == -1) {
            // recordId 无效（没有传递或值为-1），提示并关闭页面
            Toast.makeText(this, "记录不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ===== 在后台线程查询数据库 =====
        // Room 禁止主线程访问数据库，所以用 executor.execute 切到后台线程
        executor.execute(() -> {
            // 通过 DAO 根据 id 查询单条记录
            MealRecord record = AppDatabase.getInstance(this).mealDao().getRecordById(recordId);
            if (record == null) {
                // 记录不存在（可能已被删除），切回主线程提示并关闭
                runOnUiThread(() -> {
                    Toast.makeText(this, "记录不存在", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            // ===== 所有 UI 更新都切回主线程 =====
            // Android 规定只有主线程才能更新界面
            runOnUiThread(() -> {
                // 显示食物名称，null 时兜底显示"未知"
                tvFoodName.setText(record.getFoodName() != null ? record.getFoodName() : "未知");
                // 显示餐类
                tvMealType.setText(record.getMealType() != null ? record.getMealType() : "");
                // 格式化时间戳为可读的日期时间
                tvTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(new Date(record.getCreateTime())));
                // 显示备注，为空时显示"无备注"
                tvNote.setText(record.getNote() != null && !record.getNote().isEmpty()
                        ? record.getNote() : "无备注");

                // ===== 加载图片（多种组件之：多媒体组件 — 图片展示） =====
                // 使用 Glide 库加载本地文件路径的图片
                if (record.getImagePath() != null && !record.getImagePath().isEmpty()) {
                    File imgFile = new File(record.getImagePath());
                    if (imgFile.exists()) {
                        // 文件存在，用 Glide 加载到 ImageView
                        Glide.with(this).load(imgFile).into(ivPhoto);
                    } else {
                        // 文件不存在（可能被用户手动删除），显示占位图
                        ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                }

                // ===== 播放录音按钮（多种组件之：多媒体组件 — 音频播放） =====
                btnPlayAudio.setOnClickListener(v -> playAudio(record.getAudioPath()));

                // ===== 删除记录按钮 =====
                // 弹窗确认后执行删除，防止误操作
                btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("确认删除")
                            .setMessage("删除后无法恢复，确定要删除这条记录吗？")
                            .setPositiveButton("删除", (dialog, which) -> deleteRecord(recordId))
                            .setNegativeButton("取消", null)
                            .show();
                });
            });
        });
    }

    /**
     * 删除记录
     * 在后台线程执行数据库删除操作，完成后返回上一页
     * @param recordId 要删除的记录ID
     */
    private void deleteRecord(long recordId) {
        executor.execute(() -> {
            // 调用 DAO 根据 id 删除记录
            AppDatabase.getInstance(this).mealDao().deleteById(recordId);
            // 切回主线程提示并关闭页面
            runOnUiThread(() -> {
                Toast.makeText(DetailActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    /**
     * 播放录音文件
     * 使用 MediaPlayer 加载音频路径并播放
     * @param audioPath 录音文件的本地路径
     */
    private void playAudio(String audioPath) {
        // 检查录音路径是否为空
        if (audioPath == null || audioPath.isEmpty()) {
            Toast.makeText(this, "无录音", Toast.LENGTH_SHORT).show();
            return;
        }
        // 释放之前的 MediaPlayer 资源
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            // 设置数据源 → 准备 → 开始播放
            mediaPlayer.setDataSource(audioPath);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 页面销毁时释放资源
     * 关闭线程池、释放 MediaPlayer，防止内存泄漏
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();  // 关闭线程池
        if (mediaPlayer != null) {
            mediaPlayer.release();  // 释放音频播放器
            mediaPlayer = null;
        }
    }
}