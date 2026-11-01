-- RDS MySQL 8.0 초기화 SQL
-- 실행 방법: mysql -h <RDS_ENDPOINT> -u admin -p < rds-init.sql
-- 또는 MySQL Workbench / DBeaver 로 RDS에 접속 후 실행

-- ── 1. 데이터베이스 생성 ──────────────────────────────────────
CREATE DATABASE IF NOT EXISTS quoteme
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- ── 2. 애플리케이션 전용 유저 생성 ───────────────────────────
-- '%' : EC2에서 접속 허용 (RDS 보안 그룹으로 IP 제한)
CREATE USER IF NOT EXISTS 'quoteme_user'@'%' IDENTIFIED BY 'strong_password_here';

-- ── 3. 권한 부여 (quoteme DB에만 한정) ───────────────────────
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP,
      REFERENCES, CREATE TEMPORARY TABLES
  ON quoteme.*
  TO 'quoteme_user'@'%';

FLUSH PRIVILEGES;

-- ── 4. 확인 ──────────────────────────────────────────────────
SHOW DATABASES;
SELECT user, host FROM mysql.user WHERE user = 'quoteme_user';
