# 用户管理系统

## 功能

- 用户注册（密码加密后存储）
- 用户登录，返回随机会话标识 `sessionId`
- 查看当前登录用户（请求头携带 `sessionId`）
- 查看用户列表
- 修改用户名
- 修改密码
- 注销账号

## 接口

| 功能 | 方法 | 地址 | 需要登录 | 成功状态码 |
|---|---|---|---|---|
| 注册 | POST | `/api/users` | 否 | 201 |
| 登录 | POST | `/api/sessions` | 否 | 200 |
| 当前用户 | GET | `/api/users/me` | 是 | 200 |
| 用户列表 | GET | `/api/users` | 是 | 200 |
| 修改用户名 | PATCH | `/api/users/me` | 是 | 200 |
| 修改密码 | PUT | `/api/users/me/password` | 是 | 200 |
| 注销 | DELETE | `/api/users/me` | 是 | 204 |

错误状态码：

- 400：用户名或密码为空、原密码错误
- 401：未登录、sessionId 无效
- 404：用户不存在
- 409：用户名已存在

请求示例：

```http
POST /api/users
Content-Type: application/json

{"username":"alice","password":"pass123","email":"alice@test.com"}
```

## 怎么运行

1. 安装 JDK 21 和 MySQL 8，启动 MySQL
2. 建库并建表（可以用 Navicat 或命令行执行）：

```sql
CREATE DATABASE db002 DEFAULT CHARACTER SET utf8mb4;

USE db002;

CREATE TABLE users(
    user_id VARCHAR(64) PRIMARY KEY,
    user_name VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. 修改 `src/main/resources/application.properties` 里的数据库账号和密码
4. 在项目目录执行：

```bash
mvn spring-boot:run
```

5. 看到下面这行就说明启动成功：

```
Tomcat started on port 8080
```

## 说明

- 登录成功后，服务端生成一个随机的 `sessionId`，并用一个 `Map` 保存 `sessionId` 和 `userId` 的对应关系
- 需要登录的接口从请求头读取 `sessionId`，查不到就返回 401
- `sessionId` 存在内存里，服务重启后失效，需要重新登录
- 密码用 BCrypt 加密后存进数据库，登录时用 `matches()` 比对，数据库里看不到明文

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
