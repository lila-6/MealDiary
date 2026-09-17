package com.example.mealdiary.data.remote;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // 查询所有记录（若依默认POST）
    @POST("/diet/record/list")
    Call<Map<String, Object>> getRecords(@Body Map<String, Object> params);

    // 上传单条记录
    @POST("/diet/record/add")
    Call<Map<String, Object>> uploadRecord(@Body Map<String, Object> record);

    // 批量上传
    @POST("/diet/record/addBatch")
    Call<Map<String, Object>> uploadRecords(@Body List<Map<String, Object>> records);
}