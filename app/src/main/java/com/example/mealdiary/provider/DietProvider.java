package com.example.mealdiary.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.mealdiary.data.local.database.AppDatabase;
import com.example.mealdiary.data.local.entity.MealRecord;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 饮食数据共享提供者（四大组件之一：ContentProvider）
 *
 * 作用：将本 App 的饮食记录数据以 ContentProvider 方式暴露给外部应用，
 * 其他 App 可通过 ContentResolver 查询饮食数据，实现跨应用数据共享。
 *
 * URI 格式：
 * - content://com.example.mealdiary.provider/meal_record         查询所有记录
 * - content://com.example.mealdiary.provider/meal_record/1       查询 id=1 的记录
 *
 * 注册信息（在 AndroidManifest.xml 中）：
 * - authorities="com.example.mealdiary.provider"：URI 权限标识
 * - exported="true"：允许外部应用访问（这是 ContentProvider 的核心特性）
 *
 * 当前只实现了 query 方法（查询），insert/update/delete 仅做空实现，
 * 因为本项目只需展示数据共享查询能力，外部应用无需写入。
 *
 * 演示方式：在"我的"页面点击"ContentProvider 数据共享演示"按钮，
 * 内部通过 ContentResolver.query() 调用此 Provider，弹窗展示查询结果。
 */
public class DietProvider extends ContentProvider {

    // URI 权限标识，与 AndroidManifest.xml 中注册的一致
    private static final String AUTHORITY = "com.example.mealdiary.provider";
    // 表名，用于 URI 匹配
    private static final String TABLE_NAME = "meal_record";
    // URI 匹配码：查询所有记录
    private static final int CODE_ALL_RECORDS = 1;
    // URI 匹配码：查询单条记录
    private static final int CODE_SINGLE_RECORD = 2;

    /**
     * URI 匹配器
     * 用于解析 URI 并返回对应的匹配码，区分查询所有记录还是单条记录
     */
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        // content://com.example.mealdiary.provider/meal_record → CODE_ALL_RECORDS
        uriMatcher.addURI(AUTHORITY, TABLE_NAME, CODE_ALL_RECORDS);
        // content://com.example.mealdiary.provider/meal_record/123 → CODE_SINGLE_RECORD
        uriMatcher.addURI(AUTHORITY, TABLE_NAME + "/#", CODE_SINGLE_RECORD);
    }

    // Room 数据库实例
    private AppDatabase db;
    // 后台线程池，用于异步查询数据库（Room 禁止主线程访问）
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * ContentProvider 创建时初始化
     * 获取 Room 数据库实例，返回 true 表示初始化成功
     */
    @Override
    public boolean onCreate() {
        db = AppDatabase.getInstance(getContext());
        return true;
    }

    /**
     * 查询方法 — 核心功能
     *
     * 外部应用通过 ContentResolver.query(URI) 调用此方法
     * 使用 MatrixCursor 封装 Room 查询结果，返回给调用方
     *
     * @param uri           查询 URI（区分全部/单条）
     * @param projection    要返回的列（未使用，返回全部列）
     * @param selection     筛选条件（未使用）
     * @param selectionArgs 筛选参数（未使用）
     * @param sortOrder     排序方式（未使用）
     * @return MatrixCursor 包含查询结果的游标对象
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // 匹配 URI，判断是查询全部还是单条
        int match = uriMatcher.match(uri);

        // 创建 MatrixCursor，手动指定列名（顺序必须与 addRow 一致）
        MatrixCursor cursor = new MatrixCursor(new String[]{
                "_id", "userId", "mealType", "foodName", "imagePath", "audioPath", "note", "createTime", "syncStatus"
        });

        if (match == CODE_ALL_RECORDS) {
            // 查询所有记录：后台线程调用 Room，用 Future 获取同步结果
            Future<List<MealRecord>> future = executor.submit(() -> db.mealDao().getAllRecords());
            try {
                List<MealRecord> records = future.get();  // 阻塞等待后台查询完成
                for (MealRecord r : records) {
                    // 每条记录添加一行数据
                    cursor.addRow(new Object[]{
                            r.getId(), r.getUserId(), r.getMealType(), r.getFoodName(),
                            r.getImagePath(), r.getAudioPath(), r.getNote(), r.getCreateTime(), r.getSyncStatus()
                    });
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        } else if (match == CODE_SINGLE_RECORD) {
            // 查询单条记录：从 URI 路径末尾提取 id
            long id = Long.parseLong(uri.getLastPathSegment());
            Future<MealRecord> future = executor.submit(() -> db.mealDao().getRecordById(id));
            try {
                MealRecord r = future.get();
                if (r != null) {
                    cursor.addRow(new Object[]{
                            r.getId(), r.getUserId(), r.getMealType(), r.getFoodName(),
                            r.getImagePath(), r.getAudioPath(), r.getNote(), r.getCreateTime(), r.getSyncStatus()
                    });
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return cursor;
    }

    // ============================================================
    //  以下方法本项目中不需要，但继承 ContentProvider 必须重写，留空即可
    // ============================================================

    /**
     * 返回 MIME 类型，本项目未使用
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    /**
     * 插入数据，本项目未使用（外部应用不需要写入）
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    /**
     * 删除数据，本项目未使用
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    /**
     * 更新数据，本项目未使用
     */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        return 0;
    }
}