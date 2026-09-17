CREATE TABLE diet_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    meal_type VARCHAR(20) COMMENT '餐类：breakfast/lunch/dinner/snack',
    food_name VARCHAR(100) COMMENT '食物名称',
    image_path VARCHAR(255) COMMENT '图片路径',
    audio_path VARCHAR(255) COMMENT '录音路径',
    note VARCHAR(500) COMMENT '文字备注',
    create_time BIGINT COMMENT '创建时间戳',
    sync_status INT DEFAULT 0 COMMENT '同步状态：0未同步，1已同步'
) COMMENT '饮食记录表';