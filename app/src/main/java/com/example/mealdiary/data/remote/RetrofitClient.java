package com.example.mealdiary.data.remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit 网络客户端单例类（多种组件之：网络请求组件）
 * Retrofit 开源库提供的 API 接口
 * 作用：管理与若依后端的 HTTP 通信，提供统一的 API 接口访问入口
 *
 * 使用技术：
 * - Retrofit：Android 网络请求框架，封装了 OkHttp
 * - GsonConverterFactory：将 JSON 自动转换为 Java 对象（Map）
 *
 * BASE_URL 说明：
 * - 不硬编码在源码中，通过 BuildConfig.BASE_URL 注入
 * - 具体值配置在项目根目录 local.properties 的 BASE_URL 字段
 * - local.properties 已被 .gitignore 忽略，不会提交到 Git
 * - 切换环境（模拟器 / 真机 / 不同 WiFi）只需改 local.properties，无需改代码
 *
 * 为什么用单例模式：
 * - 整个 App 只需要一个 Retrofit 实例
 * - 避免重复创建，节省资源
 * - 全局统一管理 API 接口
 */
public class RetrofitClient {

    /**
     * 若依后端的访问地址
     * 由 build.gradle.kts 通过 buildConfigField 注入
     * 来源：local.properties 中的 BASE_URL 字段
     * 例如：BASE_URL=http://192.xxx.xxx.xxx/
     * 注意：必须与若依后端启动端口一致（本项目若依运行在 80 端口）
     */
    private static final String BASE_URL = com.example.mealdiary.BuildConfig.BASE_URL;
    // Retrofit 实例，全局唯一
    private static Retrofit retrofit;
    // ApiService 接口的实现类，Retrofit 自动生成
    private static ApiService apiService;

    /**
     * 获取 ApiService 接口实例（单例）
     *
     * 使用方式：
     * ApiService api = RetrofitClient.getApiService();
     * api.uploadRecord(recordMap).execute();
     *
     * @return ApiService 接口实现类，可直接调用定义的 API 方法
     */
    public static ApiService getApiService() {
        if (apiService == null) {
            // 创建 Retrofit 实例
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)                              // 设置基础 URL
                    .addConverterFactory(GsonConverterFactory.create())  // 添加 JSON 解析器
                    .build();
            // 创建 ApiService 接口的实现类（动态代理）
            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }

    /**
     * 动态修改 BaseUrl（可选，用于真机演示时灵活切换地址）
     *
     * 使用场景：演示时如果 IP 变了，可以临时调用此方法
     * RetrofitClient.setBaseUrl("http://新的IP/");
     *
     * @param baseUrl 新的后端地址
     */
    public static void setBaseUrl(String baseUrl) {
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
    }
}