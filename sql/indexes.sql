-- 直播列表与回调查询索引脚本
-- 执行前请先确认目标表结构，若索引已存在（含同名或等价前缀索引）请勿重复添加。
-- 建议在业务低峰期执行，并先备份相关表。
-- 查看现有索引：
--   SHOW INDEX FROM live_session;
--   SHOW INDEX FROM live_room;
--   SHOW INDEX FROM live_message;

-- 公开直播列表：WHERE s.status = 1 AND r.status <> 2 ORDER BY s.start_time DESC, s.id DESC
-- 复合索引可直接覆盖状态过滤与排序，避免全表扫描 + 文件排序
ALTER TABLE `live_session`
    ADD KEY `idx_session_status_start_time` (`status`, `start_time`, `id`);

-- 直播间详情 / 主播历史：按 room_id 或 user_id 过滤后按场次状态与时间排序
ALTER TABLE `live_session`
    ADD KEY `idx_session_room_status_start` (`room_id`, `status`, `start_time`, `id`),
    ADD KEY `idx_session_user_create` (`user_id`, `create_time`, `id`);

-- on_unpublish 按 client_id 查最新场次；断流宽限期与对账按 stream_name + status 查场次
ALTER TABLE `live_session`
    ADD KEY `idx_session_client` (`client_id`, `id`),
    ADD KEY `idx_session_stream_status` (`stream_name`, `status`, `id`);

-- 管理端直播间列表：按状态筛选并按创建时间倒序分页
-- 关键词仍为 LIKE '%kw%' 语义（用户已确认不改动），这里只优化状态与排序
ALTER TABLE `live_room`
    ADD KEY `idx_room_status_create` (`status`, `create_time`, `id`);

-- 申请开播使用 SELECT ... FOR UPDATE 按 user_id 加行锁；
-- 若 live_room 已有 user_id 的唯一索引可跳过本行
ALTER TABLE `live_room`
    ADD KEY `idx_room_user` (`user_id`);

-- 聊天历史按 (session_id, seq) 游标翻页、批量落库幂等，
-- 已由 sql/unique_constraints.sql 中的 uk_message_session_seq 覆盖，无需重复创建
-- ALTER TABLE `live_message`
--     ADD KEY `idx_message_session_seq` (`session_id`, `seq`);
