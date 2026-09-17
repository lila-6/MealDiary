# 三餐打卡 - 后端模块（Backend）

基于 **若依（RuoYi）框架** 开发的饮食记录后端服务，为 Android 客户端提供数据同步接口。本目录仅包含自定义开发的核心模块（饮食记录管理），完整的若依框架请从 [RuoYi 官方仓库](https://gitee.com/y_project/RuoYi) 获取。

---

## 一、技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| SpringBoot | 4.0.3 | 基础框架 |
| 若依（RuoYi） | 4.8.3 | 快速开发框架 |
| MySQL | 8.0+ | 数据存储 |
| Redis | 5.0+ | 缓存和会话管理 |
| MyBatis | 3.5.19 | ORM 框架 |
| Druid | 1.2.28 | 数据库连接池 |

---

## 二、目录结构

本目录只包含自定义开发的核心文件，部署时需放入若依项目对应位置。

```text
MealDiary/
└── backend/
    ├── README.md
    ├── sql/
    │   ├── diet_record.sql              ← 建表 SQL
    │   └── menu.sql                     
    └── src/
        └── main/
            ├── java/
            │   └── com/
            │       └── ruoyi/
            │           └── diet/
            │               ├── controller/
            │               │   └── DietRecordController.java
            │               ├── domain/
            │               │   └── DietRecord.java
            │               ├── mapper/
            │               │   └── DietRecordMapper.java
            │               └── service/
            │                   ├── IDietRecordService.java
            │                   └── impl/
            │                       └── DietRecordServiceImpl.java
            └── resources/
                ├── mapper/
                │   └── diet/
                │       └── DietRecordMapper.xml
                └── templates/
                    └── diet/
                        ├── add.html
                        ├── edit.html
                        └── record.html
```

---

## 三、数据库设计

### 表名：`diet_record`

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | BIGINT (PK, AUTO) | 主键自增 |
| `user_id` | BIGINT | 用户ID，用于数据隔离 |
| `meal_type` | VARCHAR(20) | 餐类（breakfast/lunch/dinner/snack） |
| `food_name` | VARCHAR(100) | 食物名称 |
| `image_path` | VARCHAR(255) | 图片本地路径 |
| `audio_path` | VARCHAR(255) | 录音本地路径 |
| `note` | VARCHAR(500) | 文字备注 |
| `create_time` | DATETIME | 创建时间 |
| `sync_status` | INT | 同步状态（0=未同步，1=已同步） |

### 建表 SQL

```sql
CREATE TABLE diet_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    meal_type VARCHAR(20) COMMENT '餐类：breakfast/lunch/dinner/snack',
    food_name VARCHAR(100) COMMENT '食物名称',
    image_path VARCHAR(255) COMMENT '图片路径',
    audio_path VARCHAR(255) COMMENT '录音路径',
    note VARCHAR(500) COMMENT '文字备注',
    create_time DATETIME COMMENT '创建时间',
    sync_status INT DEFAULT 0 COMMENT '同步状态：0未同步，1已同步'
) COMMENT '饮食记录表';
```

**说明：** 用户数据由 Android 客户端本地 Room 数据库管理，后端不存储 App 用户信息。若依自带的 `sys_user` 表仅用于后台管理系统。

## 四、接口文档

基础路径：所有接口以 `/diet/record` 为前缀。若依默认运行在 80 端口。

### 接口 1：上传饮食记录

- 请求：`POST /diet/record/add`
- Content-Type：`application/json`

请求体：

```json
{
  "userId": 1,
  "mealType": "breakfast",
  "foodName": "米饭",
  "imagePath": "/data/user/0/com.example.mealdiary/files/Pictures/IMG_123.jpg",
  "audioPath": "/data/user/0/com.example.mealdiary/files/Music/AUD_123.3gp",
  "note": "备注文字",
  "createTime": "2026-05-19 12:00:00",
  "syncStatus": 1
}
```

返回：

```json
{
  "code": 200,
  "msg": "操作成功"
}
```

### 接口 2：查询饮食记录列表

- 请求：`POST /diet/record/list`
- 返回：分页数据

```json
{
  "total": 10,
  "rows": [
    {
      "id": 1,
      "userId": 1,
      "mealType": "breakfast",
      "foodName": "米饭",
      "createTime": "2026-05-19 12:00:00"
    }
  ]
}
```

### 接口 3：删除饮食记录

- 请求：`POST /diet/record/remove`
- 参数：`ids=1,2,3`

## 五、部署步骤
1. 环境准备
JDK 21、MySQL 8.0（已启动）、Redis 5.0（已启动）、Maven、IDEA。

2. 获取若依框架
从 Gitee 下载若依不分离版：git clone https://gitee.com/y_project/RuoYi.git
用 IDEA 打开，等待 Maven 依赖下载完成。

3. 初始化数据库
在 MySQL 中新建数据库 ruoyi，字符集 utf8mb4，按顺序执行：
sql/ry_2024xxxx.sql（若依基础表）
sql/quartz.sql（定时任务表）
sql/diet_record.sql（本项目建表 SQL）
sql/menu.sql（若依后台菜单配置，可选）

4. 配置数据库连接
修改 ruoyi-admin/src/main/resources/application-druid.yml：
master:
    url: jdbc:mysql://localhost:3306/ruoyi?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
    username: root
    password: 你的密码

5. 部署自定义模块
将本目录 src/main/java/com/ruoyi/diet/ 下的所有文件复制到：ruoyi-admin/src/main/java/com/ruoyi/diet/
将 src/main/resources/mapper/diet/DietRecordMapper.xml 复制到：ruoyi-admin/src/main/resources/mapper/diet/
将本目录 src/main/resources/templates/diet/ 下的所有 HTML 文件复制到：
ruoyi-admin/src/main/resources/templates/diet/

6. 启动项目
在 IDEA 中运行 ruoyi-admin/src/main/java/com/ruoyi/RuoYiApplication.java。
控制台出现以下内容即为成功：(♥◠‿◠)ﾉﾞ  若依启动成功   ლ(´ڡ`ლ)ﾞ

7. 导入后台菜单（可选）
执行 sql/menu.sql，在若依后台"系统工具 → 饮食记录"中可查看同步数据。

## 六、关键改造说明
| 修改文件 | 修改内容 | 原因 |
|----------|----------|------|
| `DietRecordController.java` | 添加 `@Anonymous` 注解 | 允许 App 匿名访问，无需登录 |
| `DietRecordController.java` | 删除所有 `@RequiresPermissions` 注解 | 避免权限拦截 |
| `DietRecordController.java` | addSave 方法改为接收 `Map<String, Object>` | 处理 App 传来的字符串时间格式 |
| `application-druid.yml` | 修改数据库账号密码 | 适配本地环境 |
| `application.yml` | 调整 mapperLocations 路径 | 匹配 XML 文件位置 |


## 七、注意事项

1. createTime 格式：客户端传的是 yyyy-MM-dd HH:mm:ss 字符串，不是毫秒时间戳。后端 DietRecord 实体继承自 BaseEntity，createTime 为 java.util.Date 类型，Spring 自动解析字符串。
2. 图片/音频文件不同步：当前版本仅同步文本字段，多媒体文件保存在手机本地私有目录。
3. 同步为单向：客户端 → 后端，暂不支持后端数据回写到客户端。
4. 生产环境建议：@Anonymous 应改为 Token 鉴权，HTTP 应改为 HTTPS。

## 八、相关仓库
Android 客户端：位于本仓库根目录（../app/）
若依框架官方：https://gitee.com/y_project/RuoYi

## 九、作者
开发者：lila-6