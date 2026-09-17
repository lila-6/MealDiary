# 三餐打卡（MealDiary）
基于 Android + 若依（RuoYi）框架的饮食记录 App，支持图片/录音记录、日历查看、摇一摇随机选餐、饮食分析、三餐提醒、云端同步等功能。项目完整运用 Android 四大组件及多媒体、传感器、网络请求等多种技术。
> 本仓库包含：
> - Android 客户端（根目录下的 `app/`）
> - 若依后端自定义模块（`backend/`，详见 [backend/README.md](backend/README.md)）

---

## 一、技术栈

| 分类 | 技术 | 说明 |
|------|------|------|
| 开发语言 | Java | 纯 Java 开发 |
| 开发工具 | Android Studio | 2025.2.3 |
| 最低版本 | API 26（Android 8.0） | 目标版本 API 36 |
| UI 框架 | ViewPager2 + Fragment + BottomNavigationView | 主界面容器 |
| 本地数据库 | Room 2.6.1 | SQLite ORM 框架 |
| 网络请求 | Retrofit 2.9.0 + OkHttp + Gson | 与若依后端通信 |
| 图片加载 | Glide 4.16.0 | 高效缓存 |
| 日历控件 | Applandeo CalendarView 1.9.2 | 按月查看记录 |
| 后端 | 若依（RuoYi）+ SpringBoot | 数据同步 |

---

## 二、功能模块

| 模块 | 功能 |
|------|------|
| **用户模块** | 注册、登录、自动登录、退出登录、账号注销 |
| **饮食记录** | 选择图片、录音、文字备注、三餐分类、日历补录 |
| **饮食日记** | 日历视图、按日期查看记录、点击查看详情、删除记录 |
| **健康分析** | 连续打卡天数、近期解锁食物、本周饮食趋势、漏餐分析 |
| **三餐提醒** | 自定义早/午/晚餐提醒时间，到点弹通知 |
| **摇一摇选餐** | 加速度传感器检测摇晃，随机推荐历史记录 |
| **数据同步** | 网络恢复时自动上传到若依后端 |
| **数据共享** | ContentProvider 跨应用查询饮食数据 |
| **个人中心** | 头像/昵称/简介/性别编辑、数据看板、通用设置 |

---

## 三、项目结构
````text
MealDiary/
├── app/                                # Android 客户端
│   └── src/main/java/com/example/mealdiary/
│       ├── data/
│       │   ├── local/                  # Room 数据库、SessionManager
│       │   │   ├── entity/             # MealRecord、User
│       │   │   ├── dao/                # MealDao、UserDao
│       │   │   ├── database/           # AppDatabase
│       │   │   └── SessionManager.java
│       │   └── remote/                 # Retrofit 网络层
│       │       ├── ApiService.java
│       │       └── RetrofitClient.java
│       ├── ui/                         # 界面层
│       │   ├── MainActivity.java
│       │   ├── login/
│       │   ├── diary/
│       │   ├── home/
│       │   ├── message/
│       │   ├── profile/
│       │   └── shake/
│       ├── service/                    # SyncService、广播接收器
│       └── provider/                   # DietProvider
│
└── backend/                            # 若依后端自定义模块
    ├── README.md
    ├── sql/
    └── src/
````
---

## 四、四大组件运用

| 组件 | 实现类 | 功能 |
|------|--------|------|
| **Activity** | 8 个 | 登录、注册、主界面、添加记录、详情、摇一摇、编辑资料、账号安全 |
| **Service** | `SyncService` | 前台服务，后台将未同步记录上传到若依后端 |
| **BroadcastReceiver** | `NetworkChangeReceiver` | 静态注册展示标准用法，监听网络变化触发同步 |
| | `MealReminderReceiver` | 接收 AlarmManager 闹钟广播，弹出三餐提醒通知 |
| | `MainActivity` 动态 Receiver | 绕过 Android 7.0+ 限制，实际监听网络变化 + 切换 Tab |
| **ContentProvider** | `DietProvider` | 向外部应用暴露饮食记录查询接口（query 方法） |

---

## 五、多种组件运用

| 类型 | 具体使用 | 数量 |
|------|----------|:---:|
| **多媒体** | 图片选择（ContentResolver）、录音（MediaRecorder）、播放（MediaPlayer） | 3 |
| **传感器** | 加速度传感器（摇一摇随机选餐） | 1 |
| **网络请求** | Retrofit + OkHttp（与若依后端通信） | 1 |
| **数据存储** | Room 数据库 + SharedPreferences | 1 |

---

## 六、数据库设计

### meal_record 表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INTEGER (PK, AUTO) | 主键自增 |
| `userId` | INTEGER | 用户ID，数据隔离 |
| `mealType` | TEXT | breakfast/lunch/dinner/snack |
| `foodName` | TEXT | 食物名称 |
| `imagePath` | TEXT | 图片本地路径 |
| `audioPath` | TEXT | 录音本地路径 |
| `note` | TEXT | 文字备注 |
| `createTime` | INTEGER | 毫秒时间戳 |
| `syncStatus` | INTEGER | 0=未同步，1=已同步 |

### user 表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INTEGER (PK, AUTO) | 主键自增 |
| `username` | TEXT | 用户名 |
| `password` | TEXT | 密码 |
| `email` | TEXT | 邮箱 |

**说明：** 个人信息（昵称、头像路径、简介、性别）存储在 SharedPreferences，Key 格式 `userId_nickname`，实现多用户数据隔离。

---

## 七、核心难点与解决方案

### 1. Room 禁止主线程访问数据库
**问题：** 主线程直接操作 Room 抛 `IllegalStateException`。
**解决：** 所有数据库操作放入 `new Thread()` 或 `ExecutorService`，通过 `Handler(Looper.getMainLooper())` 或 `runOnUiThread()` 切回主线程更新 UI。

### 2. ViewPager2 中 Fragment 生命周期冲突
**问题：** ViewPager2 滑动导致 Fragment 频繁 `onDestroy`，线程池被提前关闭，抛 `RejectedExecutionException`。
**解决：** Fragment 中弃用 `ExecutorService`，改用 `new Thread()`，不受生命周期管理影响。

### 3. HTTP 明文请求被禁止
**问题：** Android 9+ 默认禁止 HTTP 明文传输。
**解决：** 在 `AndroidManifest.xml` 中声明 `android:usesCleartextTraffic="true"`，仅用于开发调试。

### 4. 多用户数据隔离
**问题：** 切换账号后，新账号显示旧账号的个人信息。
**解决：** SharedPreferences 的 Key 从固定字符串改为 `userId + "_nickname"` 格式。

### 5. 账户注销需彻底清除数据
**问题：** 注销只清除登录状态，数据残留。
**解决：** 依次删除饮食记录、用户账号、SharedPreferences 数据，所有操作在同一后台线程顺序执行。

---

## 八、运行说明

### 环境要求
- Android Studio 2025.2.3 或更高
- JDK 21
- Android SDK API 36

### 运行步骤
1. 克隆本项目到本地
2. 用 Android Studio 打开项目根目录
3. 等待 Gradle 同步完成
4. 修改 `RetrofitClient.java` 中的 `BASE_URL` 为你的后端地址：
   ```java
   // 模拟器
   private static final String BASE_URL = "http://10.0.2.2:80/";
   // 真机（替换为电脑局域网IP）
   private static final String BASE_URL = "http://192.168.x.x:x/";
5. 手机和电脑连同一 WiFi（或手机开热点电脑连接）
6. 启动若依后端（参考 backend/README.md）
7. 运行 App 即可

### 后端部署
本项目后端基于若依（RuoYi）框架，部署说明见 backend/README.md。


## 九、注意事项
1. 同步条件：手机和电脑需在同一网络下才能同步，因为后端部署在本地电脑
2. 同步方向：当前为单向（客户端 → 后端），不支持云端数据回写
3. 同步内容：仅同步文本字段（userId、mealType、foodName、createTime），图片和音频文件保留在本地
4. 密码存储：实训版明文存储，正式环境应使用 MD5/SHA 加密
5. 闹钟重启：Android 重启后闹钟失效，需用户重新设置

## 十、相关仓库
后端模块：backend/README.md
若依框架官方：https://gitee.com/y_project/RuoYi


## 十一、作者
开发者：lila-6