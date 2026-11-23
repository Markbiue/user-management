# 用户管理系统

基于 Java 21 和 Spring Boot 的用户管理系统，实现了注册、登录、当前用户、用户列表、修改用户名、修改密码、注销七个接口。

## 功能

- 用户注册（密码 BCrypt 加密后存储）
- 用户登录，返回 `sessionId` 和 `token`
- 查看当前登录用户（支持 `sessionId` 或 `Authorization: Bearer <token>`）
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

- 400：参数为空、格式不合法、原密码错误
- 401：未登录、token 无效、用户名或密码错误
- 404：接口路径不存在
- 409：用户名已存在

请求示例：

```http
POST /api/users
Content-Type: application/json

{"username":"alice","password":"pass123","email":"alice@test.com"}
```

## 项目结构

```
src/main/java/com/zeqi/usermanagement
├── controller   接收请求、组装响应
├── service      业务逻辑
├── repository   数据库操作
├── entity       表映射
├── dto          请求参数，带校验注解
├── util         JWT 工具
├── interceptor  JWT 拦截器
└── config       Spring 配置
```

## 运行

1. 安装 JDK 21 和 MySQL 8，启动 MySQL
2. 建库建表：

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

3. 配置环境变量（数据库密码和 JWT 密钥不写在代码里）

PowerShell：

```powershell
$env:DB_PASSWORD="你的数据库密码"
$env:JWT_SECRET="至少32位的随机字符串"
```

IDEA：Run → Edit Configurations → Environment variables 里填这两项。

可选变量：

- `DB_USERNAME`：数据库用户名，默认 `root`
- `DB_URL`：数据库连接串，默认连本机 `db002`

4. 启动：

```bash
mvn spring-boot:run
```

5. 看到这行说明启动成功：

```
Tomcat started on port 8080
```

## 说明

- 登录成功后生成随机的 `sessionId`，服务端用 `Map` 保存 `sessionId` 和 `userId` 的对应关系
- 同时返回 JWT，客户端可以用 `Authorization: Bearer <token>` 访问需要登录的接口
- 两种方式都支持：带 token 用 token，没带 token 则回退到 `sessionId`
- `sessionId` 存在内存里，服务重启后失效
- 密码用 BCrypt 加密后存库，登录时用 `matches()` 比对，数据库里看不到明文
- 参数使用 DTO 接收，`@NotBlank`、`@Size` 做校验，校验失败返回 400

## 接口测试

使用 Postman 测试全部接口，测试结果截图见提交的 PDF 文件。
