ALTER TABLE user_settings DROP FOREIGN KEY fk_user_settings_user;
ALTER TABLE user_settings ADD CONSTRAINT fk_user_settings_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE routine_tasks DROP FOREIGN KEY fk_routine_tasks_user;
ALTER TABLE routine_tasks ADD CONSTRAINT fk_routine_tasks_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE nap_sessions DROP FOREIGN KEY fk_nap_sessions_user;
ALTER TABLE nap_sessions ADD CONSTRAINT fk_nap_sessions_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
