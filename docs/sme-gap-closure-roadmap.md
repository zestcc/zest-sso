# 中小企业差距闭环路线图

> 在 M0~M4 能力已验收通过的基础上，补齐「国内 IdP、集成成本、运维傻瓜化、合规背书」四类中小企业真实痛点。

## 总览

| 类别 | 交付物 | 状态 |
|------|--------|------|
| 国内 IdP | [domestic-idp-guide.md](domestic-idp-guide.md) + 插拔式 `adapterKey` SPI | ✅ 文档 + 适配器 |
| RP 集成 | [rp-integration-cookbook.md](rp-integration-cookbook.md) + `docs/templates/rp/*` | ✅ Cookbook + 模板 |
| 生产运维 | `deploy/helm/` + `deploy/monitoring/` + `scripts/backup-prod.*` | ✅ 可部署 |
| 合规材料 | [compliance/](compliance/README.md) 材料包 | ✅ 自查 + 测评准备 |

## 阶段计划

### 阶段 A — 立即可用（当前交付）

- [x] 飞书 / 钉钉 / 企微 **插拔式联邦适配器**（`FederatedIdpAdapter` SPI + `GET /adapters`）
- [x] 飞书 OIDC 联邦配置模板与 Admin API 注册脚本（`adapterKey`）
- [x] 钉钉 / 企业微信接入说明（Discovery 或 `endpoint_config` 手动端点）
- [x] Spring Boot / Vue / Node RP 集成模板
- [x] Helm Chart 单机/小集群部署
- [x] MySQL + Redis 备份脚本
- [x] Grafana 大盘 + Prometheus 告警规则
- [x] 等保测评材料清单、渗透测试范围说明书

### 阶段 B — 下一迭代（产品增强，约 2~4 周）

- [ ] 企业微信 **自定义 TokenResponseClient**（`wecom` 适配器生产就绪）
- [ ] Admin 控制台「应用接入向导」（OIDC 客户端一键生成 + 回调 URL 复制）
- [ ] 钉钉 / 企微 **告警 Webhook** 预置（登录异常、Back-Channel 失败）
- [ ] 短信 MFA（阿里云 / 腾讯云 SMS 适配器）
- [ ] Helm Chart 增加 HPA、PodDisruptionBudget

### 阶段 C — 合规闭环（需外部机构，约 4~8 周）

- [ ] 委托等保测评机构出具正式报告（二级/三级按甲方要求）
- [ ] 委托渗透测试并归档整改记录
- [ ] SOC2/ISO27001（出海客户可选）

## 验收方式

```powershell
# 1. 运维包语法检查
helm lint deploy/helm/zest-sso

# 2. 备份脚本干跑（需本地 MySQL/Redis）
powershell -File scripts/backup-prod.ps1 -DryRun

# 3. 全量功能回归
$env:MYSQL_PASSWORD='<prod>'
powershell -File scripts/acceptance.ps1

# 4. 国内 IdP 模板注册（飞书，需替换 client 凭据）
powershell -File scripts/register-idp-template.ps1 -Template feishu -ClientId <id> -ClientSecret <secret>
```

## 与 README / deployment 的关系

- 部署入口：[deployment.md](deployment.md) §7 Helm
- 集成入口：[integration-guide.md](integration-guide.md) + [rp-integration-cookbook.md](rp-integration-cookbook.md)
- 合规入口：[compliance/README.md](compliance/README.md)
