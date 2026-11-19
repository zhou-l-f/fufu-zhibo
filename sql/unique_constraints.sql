-- 唯一约束补充脚本
-- 执行前请先确认目标表中不存在重复数据，否则 ALTER 会失败。
-- 建议在业务低峰期执行，并先备份相关表。

-- 账号唯一：并发注册时由数据库兜底，避免 check-then-insert 竞态
ALTER TABLE `user`
    ADD UNIQUE KEY `uk_user_username` (`username`);

-- 实名信息：一个用户只能有一条记录，手机号与身份证号全局唯一
ALTER TABLE `user_identity`
    ADD UNIQUE KEY `uk_identity_user_id` (`user_id`),
    ADD UNIQUE KEY `uk_identity_phone_hash` (`phone_hash`),
    ADD UNIQUE KEY `uk_identity_id_card_hash` (`id_card_hash`);

-- 聊天消息：同一场次内消息序号唯一，保证批量落库幂等且不重复
ALTER TABLE `live_message`
    ADD UNIQUE KEY `uk_message_session_seq` (`session_id`, `seq`);

-- 重复数据排查参考：
-- SELECT username, COUNT(*) FROM `user` GROUP BY username HAVING COUNT(*) > 1;
-- SELECT user_id, COUNT(*) FROM user_identity GROUP BY user_id HAVING COUNT(*) > 1;
-- SELECT phone_hash, COUNT(*) FROM user_identity GROUP BY phone_hash HAVING COUNT(*) > 1;
-- SELECT id_card_hash, COUNT(*) FROM user_identity GROUP BY id_card_hash HAVING COUNT(*) > 1;
-- SELECT session_id, seq, COUNT(*) FROM live_message GROUP BY session_id, seq HAVING COUNT(*) > 1;
