package com.example.mealdiary.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.example.mealdiary.R;
import com.example.mealdiary.ui.home.HomeFragment;
import com.example.mealdiary.ui.diary.DiaryFragment;
import com.example.mealdiary.ui.message.MessageFragment;
import com.example.mealdiary.ui.profile.MineFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * 主界面 Activity — ViewPager2 容器
 *
 * 负责管理底部4个Tab页面的切换：
 * - Tab 0：HomeFragment（首页）
 * - Tab 1：DiaryFragment（饮食日记）
 * - Tab 2：MessageFragment（健康）
 * - Tab 3：MineFragment（我的）
 *
 * 内部注册了两个 BroadcastReceiver：
 * 1. SWITCH_TO_DIARY 接收器：首页点击"饮食日记"卡片时，切换到 Tab 1
 * 2. 网络变化接收器：监听网络状态，恢复连接时自动启动 SyncService 同步数据
 *
 * 使用 ViewPager2 + FragmentStateAdapter 实现 Fragment 切换，
 * BottomNavigationView 与 ViewPager2 双向联动（点击Tab切换页面，滑动页面同步Tab选中状态）
 */
public class MainActivity extends AppCompatActivity {

    // ViewPager2 用于左右滑动切换 Fragment
    private ViewPager2 viewPager;
    // 底部导航栏
    private BottomNavigationView bottomNav;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ===== 绑定控件 =====
        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottomNav);

        // ===== 准备 Fragment 列表 =====
        // 四个 Tab 分别对应四个 Fragment
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new HomeFragment());                                      // 首页
        fragments.add(new DiaryFragment());                                     // 饮食日记
        fragments.add(new MessageFragment());                                   // 健康
        // "我的"页面，传入默认值（实际数据会从 SharedPreferences 按 userId 加载，这里只是兜底）
        fragments.add(MineFragment.newInstance("张三", "zhangsan@example.com"));

        // ===== 设置 ViewPager2 适配器 =====
        // FragmentStateAdapter：管理 Fragment 的创建和切换
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public Fragment createFragment(int position) {
                return fragments.get(position);  // 根据位置返回对应的 Fragment
            }

            @Override
            public int getItemCount() {
                return fragments.size();  // 总共4个 Tab
            }
        });

        // ===== 底部导航栏点击事件 =====
        // 点击底部 Tab → ViewPager2 切换到对应页面
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) viewPager.setCurrentItem(0);       // 切换到首页
            else if (id == R.id.nav_diary) viewPager.setCurrentItem(1);  // 切换到饮食日记
            else if (id == R.id.nav_message) viewPager.setCurrentItem(2); // 切换到健康
            else if (id == R.id.nav_mine) viewPager.setCurrentItem(3);    // 切换到我的
            else return false;
            return true;
        });

        // ===== ViewPager2 滑动监听 =====
        // 滑动页面 → 底部 Tab 选中状态同步更新
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                bottomNav.getMenu().getItem(position).setChecked(true);
            }
        });

        // ===== 广播接收器1：切换 Tab注册 =====。
        // Tab切换广播是在 HomeFragment 里发送的。HomeFragment 和 MainActivity 之间没法直接通信
        // HomeFragment 点击"饮食日记"卡片时发送 "SWITCH_TO_DIARY" 广播
        // 目的：实现快捷入口里的2个按钮。收到广播后切换到饮食日记页（Tab 1）
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                viewPager.setCurrentItem(1);
            }
        }, new IntentFilter("SWITCH_TO_DIARY"), Context.RECEIVER_NOT_EXPORTED);

        // ===== App 启动时手动触发一次同步 =====
        // 确保每次打开 App 都能尝试同步未上传的记录
        Intent syncIntent = new Intent(this, com.example.mealdiary.service.SyncService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(syncIntent);  // Android 8.0+ 必须用前台服务
        } else {
            startService(syncIntent);            // 低版本用普通服务
        }
    }
}