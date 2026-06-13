# ZestSSO Admin

ZestSSO 管理控制台 — 对标 Keycloak/Okta Admin Console 的 IAM 管理能力。

## 功能模块

| 模块 | SSO_ADMIN | SSO_OPERATOR |
|------|-----------|--------------|
| 概览仪表盘 | ✓ | ✓ |
| 应用接入（OAuth Client） | ✓ | ✗ |
| 用户管理（含解锁/删除） | ✓ | ✓（仅普通用户） |
| 租户管理 | ✓ | ✗ |
| 角色管理 | ✓ | ✗ |
| 审计日志 | ✓ | ✓ |
| 系统设置 | ✓ | ✗ |
| 个人中心 / 改密 | ✓ | ✓ |

## 开发模式

```bash
# 终端 1：后端
mvn -pl zest-sso-server spring-boot:run

# 终端 2：前端热更新
cd zest-sso-admin
npm install
npm run dev
```

- 开发地址：`http://localhost:5175`（Vite 代理 `/api` → `:9000`）
- 管理员：`admin` / `admin123`
- 运维账号：`operator` / `operator123`

## 生产部署（同域集成）

```bash
# 构建前端并复制到后端 static/admin
powershell -File scripts/build-admin.ps1

# 打包后端（含 Admin 静态资源）
mvn -pl zest-sso-server -am package -DskipTests
```

启动后访问：`http://<host>:9000/admin/`

生产环境建议在 `application-prod.yml` 设置：

```yaml
zest:
  sso:
    bootstrap:
      reset-default-password: false
```

## 技术栈

Vue 3 · TypeScript · Vite · Ant Design Vue 4 · Pinia · Session Cookie 认证
