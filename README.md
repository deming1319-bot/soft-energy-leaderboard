# 知行问答 · Soft Energy Leaderboard

中华传统文化限时答题项目，包含微信小程序、管理后台和 Spring Boot API。题目由管理员发布，学员在截止前作答；系统按答案内容与顺序判定冠军、亚军，并记录提交时间。

## 项目结构

| 目录 | 用途 |
| --- | --- |
| `backend/` | Java 21 + Spring Boot API、Flyway 数据库迁移与测试 |
| `admin-web/` | Vue 3 + TypeScript + Element Plus 管理后台 |
| `miniapp/` | 微信原生小程序 + TypeScript + TDesign Miniprogram |
| `deploy/` | Nginx、systemd、Jenkins 的通用部署模板 |
| `开发文档/` | 需求、架构、测试、部署与交接记录 |

## 本地启动

需要 Java 21、Maven、Node.js 20+、pnpm 和 MySQL 8.4。根目录 `compose.yaml` 可启动仅供开发使用的 MySQL。分别在 `backend/` 运行 `mvn spring-boot:run`，在 `admin-web/` 运行 `pnpm install`、`pnpm dev`；小程序在 `miniapp/` 运行 `pnpm install`、`pnpm build:npm` 后，用微信开发者工具导入 `miniapp` 目录。

小程序开发接口默认指向 `http://127.0.0.1:8081/api/v1`。如果使用真机预览，请将 `miniapp/miniprogram/config/runtime.ts` 中的开发地址改为本机可访问的局域网地址；正式版必须改为已备案、已配置微信合法域名的 HTTPS API。

## 生产部署前必须配置

本仓库只提供模板，不包含生产凭据或可直接使用的线上地址。部署前必须在服务器环境或密钥管理系统中配置数据库、JWT、微信 AppSecret、管理员强密码、域名与 TLS 证书；使用独立生产数据库，并关闭模拟登录。生产配置校验会拒绝默认开发密码和密钥。不要把 `.env`、证书私钥或真实用户数据提交到仓库。

`Jenkinsfile` 默认只执行测试和构建；生产部署需人工开启 `DEPLOY_TO_SERVER`，并先按 `deploy/` 模板配置服务器。小程序的体验、审核和正式发布仍需在微信平台人工确认。

## 项目文档

- [文档导航](./开发文档/文档导航.md)
- [开发环境启动说明](./开发文档/开发环境启动说明.md)
- [开发进度与测试报告](./开发文档/开发进度与测试报告.md)
- [线上部署执行记录](./开发文档/线上部署执行记录.md)
- [阿里云首次部署与 Jenkins 操作手册](./开发文档/阿里云首次部署与Jenkins操作手册.md)

## 许可证

项目权利人确认开源许可证后再添加 `LICENSE`。在此之前，公开仓库仅供查看，不代表允许复制、修改或再分发。
