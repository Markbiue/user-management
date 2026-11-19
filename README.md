# 用户管理系统

择栖工作室 2026 后端招新 · 第四题

- 姓名：薛锦睿
- 学号：2026090910002
- 技术栈：Java 21 / Spring Boot 4.1.2 / Spring Data JPA / MySQL 8.4 / Maven

## 功能

**必做**

- 用户注册（密码 BCrypt 哈希存储）
- 用户登录，返回随机会话标识 `sessionId`
- 查看当前登录用户（请求头携带 `sessionId`）

**选做**

- 查看用户列表
- 修改用户名
- 修改密码
- 注销账号

## 接口

| 功能 | 方法 | URL | 认证 | 成功 |
|---|---|---|---|---|
| 注册 | POST | `/api/users` | 无 | 201 |
| 登录 | POST | `/api/sessions` | 无 | 200 |
| 当前用户 | GET | `/api/users/me` | sessionId | 200 |
| 用户列表 | GET | `/api/users` | sessionId | 200 |
| 修改用户名 | PATCH | `/api/users/me` | sessionId | 200 |
| 修改密码 | PUT | `/api/users/me/password` | sessionId | 200 |
| 注销 | DELETE | `/api/users/me` | sessionId | 204 |

错误码：400 参数错误 / 401 未登录或 sessionId 失效 / 404 用户不存在 / 409 用户名已存在。

## 数据库

库 `db002`，表 `users`：

```sql
CREATE TABLE users(
    user_id VARCHAR(64) PRIMARY KEY,
    user_name VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

配置见 `src/main/resources/application.properties`，`ddl-auto=none`（表由 SQL 手动创建）。

## 启动

```bash
# 1. 确认 MySQL 已启动，库 db002 存在
# 2. 启动
mvn spring-boot:run
```

看到以下日志即成功：

```
HikariPool-1 - Start completed.
Tomcat started on port 8080
```

## 设计说明

- **登录态**：登录成功后生成 UUID 作为 `sessionId`，服务端用 `Map` 保存 `sessionId → userId`；需要登录的接口从请求头读取并校验，查不到返回 401。
- **密码**：`BCryptPasswordEncoder` 哈希存储，数据库中是 `$2a$10$...`，登录用 `matches()` 校验。
- **限制**：session 存在内存中，服务重启后失效；多实例部署需改为 Redis。

## 测试截图

见 `docs/screenshots/`：

| 文件 | 内容 |
|---|---|
| 01-register-201.png | 注册成功 201 |
| 02-duplicate-409.png | 重复注册 409 |
| 03-login-200.png | 登录成功 200 |
| 04-me-200.png | 当前用户 200 |
| 05-unauthorized-401.png | 未登录 401 |
| 06-list-200.png | 用户列表 200 |
| 07-rename-200.png | 修改用户名 200 |
| 08-password-200.png | 修改密码 200 |
| 09-delete-204.png | 注销 204 |
