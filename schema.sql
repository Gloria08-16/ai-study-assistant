-- ============================================
-- AI智能伴学与私人知识库系统 - 数据库初始化脚本
-- ============================================

-- 创建数据库（指定字符集 utf8mb4，防止中文乱码）
CREATE DATABASE IF NOT EXISTS ai_study_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_study_db;

-- ============================================
-- 1. 聊天会话表
-- ============================================
CREATE TABLE IF NOT EXISTS chat_session (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '会话ID',
    user_id     BIGINT          NOT NULL                 COMMENT '用户ID',
    title       VARCHAR(255)    DEFAULT 'New Chat'        COMMENT '会话标题',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- ============================================
-- 2. 聊天消息表
-- ============================================
CREATE TABLE IF NOT EXISTS chat_message (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '消息ID',
    session_id  BIGINT          NOT NULL                 COMMENT '会话ID',
    role        VARCHAR(20)     NOT NULL                 COMMENT '角色：user 或 assistant',
    content     TEXT            NOT NULL                 COMMENT '消息内容',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- ============================================
-- 3. 知识库表
-- ============================================
CREATE TABLE IF NOT EXISTS knowledge_base (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '资料ID',
    user_id     BIGINT          NOT NULL                 COMMENT '用户ID',
    title       VARCHAR(255)    NOT NULL                 COMMENT '资料标题',
    content     TEXT            NOT NULL                 COMMENT '资料内容',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';
