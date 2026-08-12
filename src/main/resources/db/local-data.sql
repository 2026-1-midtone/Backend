INSERT IGNORE INTO users (id, google_subject, email, nickname, timezone)
VALUES (1, 'local-development-user', 'local@shiftrhythm.test', '로컬 사용자', 'Asia/Seoul');

INSERT IGNORE INTO user_settings (user_id, preferred_nap_minutes, max_naps_per_day)
VALUES (1, 20, 2);
