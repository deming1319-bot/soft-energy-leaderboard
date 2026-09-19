# Soft Energy Leaderboard

面向儒、释、道文化修习场景的限时问答、自动判定与排行榜系统。

## 项目组成

```text
backend/       Java 21 + Spring Boot 3.5.16 API
admin-web/     Vue 3 + TypeScript + Element Plus 管理系统
miniapp/       微信原生小程序 + TypeScript + TDesign Miniprogram
deploy/        Jenkins、Nginx、systemd 生产部署配置
开发文档/       需求、架构、UI、部署和交接文档
```

正式启动方式和开发账号见 `开发文档/开发环境启动说明.md`。生产密钥、微信 AppSecret 和数据库密码不得写入本仓库。

阿里云首次部署和 Jenkins 控制台操作见 `开发文档/阿里云首次部署与Jenkins操作手册.md`。

## 当前可看效果

- 管理端：http://localhost:5173（开发账号见启动说明）
- 后端健康检查：http://localhost:8081/api/v1/health
- 小程序：用微信开发者工具导入仓库下的 `miniapp` 目录

当前实现与自动化验证结果见 `开发文档/开发进度与测试报告.md`。
