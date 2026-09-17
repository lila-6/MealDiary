package com.example.mealdiary.data.remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit 网络客户端单例类（多种组件之：网络请求组件）
 *Retrofit 开源库提供的 API 接口
 * 作用：管理与若依后端的 HTTP 通信，提供统一的 API 接口访问入口
 *
 * 使用技术：
 * - Retrofit：Android 网络请求框架，封装了 OkHttp
 * - GsonConverterFactory：将 JSON 自动转换为 Java 对象（Map）
 *
 * BASE_URL 说明：
 * - 模拟器调试：使用 10.0.2.2（Android 模拟器访问宿主机的特殊 IP）
 * - 真机演示：使用电脑的局域网 IP（如 192.168.x.x）
 *   手机和电脑需在同一网络（手机开热点让电脑连接，或连同一 WiFi）
 *
 * 为什么用单例模式：
 * - 整个 App 只需要一个 Retrofit 实例
 * - 避免重复创建，节省资源
 * - 全局统一管理 API 接口
 */
public class RetrofitClient {

    /**
     * 若依后端的访问地址
     * - 模拟器：http://10.0.2.2/ (Android 模拟器访问宿主机的特殊 IP)
     * - 真机：请替换为你电脑的局域网 IP (如 http://192.168.x.x/)
     * 注意：必须与若依后端启动端口一致（本项目若依运行在 80 端口）
     */
    private static final String BASE_URL = "http://10.0.2.2/"; // 模拟器示例地址
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