package com.example.mealdiary.ui.diary;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.mealdiary.R;
import com.example.mealdiary.data.local.database.AppDatabase;
import com.example.mealdiary.data.local.entity.MealRecord;
import com.example.mealdiary.data.local.SessionManager;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 添加三餐记录页面
 * 功能：选择图片 + 录音 + 文字备注 + 三餐分类
 * 可从日历传入目标日期，支持补录历史记录
 */
public class AddMealActivity extends AppCompatActivity {

    // 请求码：选图片
    private static final int REQUEST_IMAGE_PICK = 200;
    // 请求码：录音权限
    private static final int REQUEST_RECORD_AUDIO = 101;
    // 日志标签
    private static final String TAG = "AddMealActivity";

    // ===== UI控件 =====
    private Spinner spinnerMealType;      // 三餐类型下拉
    private EditText etFoodName, etNote;  // 食物名称、备注
    private ImageView ivPreview;          // 图片预览
    private Button btnPickImage;          // 选图片按钮
    private Button btnRecord, btnPlay, btnSave;  // 录音、播放、保存
    private TextView tvTargetDate;        // 目标日期显示

    // ===== 数据字段 =====
    private String currentPhotoPath;      // 当前选择的图片路径
    private String currentAudioPath;      // 当前录音文件路径
    private MediaRecorder mediaRecorder;  // 录音器
    private MediaPlayer mediaPlayer;      // 播放器
    private boolean isRecording = false;  // 是否正在录音
    private long targetTimeMillis = System.currentTimeMillis();  // 目标日期时间戳，默认今天

    // 后台线程池，用于数据库操作
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_meal);

        // ===== 绑定控件 =====
        tvTargetDate = findViewById(R.id.tvTargetDate);
        spinnerMealType = findViewById(R.id.spinnerMealType);
        etFoodName = findViewById(R.id.etFoodName);
        etNote = findViewById(R.id.etNote);
        ivPreview = findViewById(R.id.ivPreview);

        btnPickImage = findViewById(R.id.btnPickImage);   // 选图片按钮
        btnRecord = findViewById(R.id.btnRecord);          // 录音按钮
        btnPlay = findViewById(R.id.btnPlay);              // 播放按钮
        btnSave = findViewById(R.id.btnSave);              // 保存按钮

        // ===== 获取从日历传入的目标日期 =====
        String targetDate = getIntent().getStringExtra("targetDate");
        if (targetDate != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = sdf.parse(targetDate);
                if (date != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    cal.set(Calendar.HOUR_OF_DAY, 12);
                    targetTimeMillis = cal.getTimeInMillis();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 显示目标日期文字
        SimpleDateFormat displaySdf = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
        if (isToday(targetTimeMillis)) {
            tvTargetDate.setText("记录日期：今天");
        } else {
            tvTargetDate.setText("记录日期：" + displaySdf.format(new Date(targetTimeMillis)));
        }

        // 三餐类型下拉选项
        String[] mealTypes = {"breakfast", "lunch", "dinner", "snack"};
        spinnerMealType.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, mealTypes));

        // ===== 选图片按钮点击 =====
        btnPickImage.setOnClickListener(v -> openGallery());

        // ===== 录音按钮点击 =====
        btnRecord.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            } else {
                toggleRecording();
            }
        });

        // ===== 播放按钮点击 =====
        btnPlay.setOnClickListener(v -> playAudio());

        // ===== 保存按钮点击 =====
        btnSave.setOnClickListener(v -> saveRecord());
    }

    /**
     * 判断给定时间戳是否是今天
     */
    private boolean isToday(long timeMillis) {
        Calendar today = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(timeMillis);
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR)
                && today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * 打开系统文件选择器，选择图片
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    /**
     * ContentProvider 消费者示例
     *
     * 用户通过系统文件选择器选中图片后，系统返回一个 Uri（指向系统相册/文件 Provider 中的数据）
     * 此方法通过 ContentResolver 读取该 Uri 指向的图片数据，复制到应用私有目录
     *
     * 这里访问了系统相册/文件 Provider（ContentProvider 消费者角色）：
     * getContentResolver().openInputStream(imageUri) → 系统 ContentProvider 返回数据流
     *
     * 为什么要复制到私有目录？
     * - 系统返回的 Uri 是临时权限，App 关闭后可能失效
     * - 复制到 getExternalFilesDir 后，图片完全由本 App 管理，不受原图影响
     *
     * @param imageUri 系统 ContentProvider 返回的图片 Uri
     * @return 复制后的本地文件路径
     */
    private String saveImageToAppStorage(Uri imageUri) {
        try {
            // 通过 ContentResolver 从系统 ContentProvider 读取图片数据
            java.io.InputStream input = getContentResolver().openInputStream(imageUri);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (storageDir != null && !storageDir.exists()) storageDir.mkdirs();
            File destFile = new File(storageDir, "IMG_" + timeStamp + ".jpg");
            java.io.FileOutputStream output = new java.io.FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > 0) {
                output.write(buffer, 0, len);
            }
            output.close();
            input.close();
            return destFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 处理选图片返回结果
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                currentPhotoPath = saveImageToAppStorage(selectedImage);
                ivPreview.setImageURI(selectedImage);
                Toast.makeText(this, "图片已选择", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 处理权限请求的结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            toggleRecording();
        }
    }

    /**
     * 切换录音状态：开始 / 停止
     */
    private void toggleRecording() {
        if (!isRecording) {
            startRecording();
            btnRecord.setText("停止录音");
        } else {
            stopRecording();
            btnRecord.setText("录音");
        }
        isRecording = !isRecording;
    }

    /**
     * 开始录音
     */
    private void startRecording() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (storageDir != null && !storageDir.exists()) storageDir.mkdirs();
        File audioFile = new File(storageDir, "AUD_" + timeStamp + ".3gp");
        currentAudioPath = audioFile.getAbsolutePath();

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(currentAudioPath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            Toast.makeText(this, "开始录音", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止录音
     */
    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            Toast.makeText(this, "录音已保存", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 播放已录制的音频
     */
    private void playAudio() {
        if (currentAudioPath == null) {
            Toast.makeText(this, "请先录音", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(currentAudioPath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "播放中...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存饮食记录到本地 Room 数据库
     */
    private void saveRecord() {
        String foodName = etFoodName.getText().toString().trim();
        if (foodName.isEmpty()) {
            Toast.makeText(this, "请输入食物名称", Toast.LENGTH_SHORT).show();
            return;
        }

        MealRecord record = new MealRecord(
                SessionManager.getInstance(this).getUserId(),
                spinnerMealType.getSelectedItem().toString(),
                foodName,
                currentPhotoPath,
                currentAudioPath,
                etNote.getText().toString().trim(),
                targetTimeMillis,
                0
        );

        executor.execute(() -> {
            long id = AppDatabase.getInstance(this).mealDao().insert(record);
            Log.d(TAG, "插入记录id=" + id);
            runOnUiThread(() -> {
                Toast.makeText(AddMealActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}