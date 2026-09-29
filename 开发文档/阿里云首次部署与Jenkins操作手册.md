# 阿里云首次部署与 Jenkins 操作手册

> 最近更新：2026-09-29
> 目标：让项目负责人按本手册完成阿里云 ECS、同机 MySQL、域名、Jenkins 和第一次后端/管理端发布。
> 安全规则：任何数据库密码、微信 AppSecret、JWT 密钥、私钥都只写在服务器或 Jenkins Credentials，不写入 GitHub 和本文件。

## 1. 当前结论

已经完成的代码侧准备：

- Jenkinsfile 同时校验 Java 后端、Vue 管理端和微信小程序；
- 普通构建只做 CI，只有人工勾选 `DEPLOY_TO_SERVER` 才发布；
- 只允许从 `main` 或 `v*` 标签发布；
- 版本发布到 `/opt/soft-energy/releases/<构建号>`，再原子切换 `current`；
- 后端或管理端健康检查失败会恢复上一版本；
- systemd 使用低权限 `softenergy` 用户运行 Java；
- 后端只监听 `127.0.0.1:8081`，不向公网开放；
- Nginx 使用 443 对外提供管理后台和小程序 API；
- Spring Boot 通过 `127.0.0.1:3306` 连接同机 MySQL，Flyway 自动执行 V1—V4 迁移；
- 小程序不会也不能直接连接 MySQL，只调用 `https://api.<域名>/api/v1`。

项目已配置格式有效的正式 AppID，朋友账号下的 GitHub 公开仓库和首次推送已经完成。还不能由开发人员代填的内容：正式域名、证书、对应 AppSecret 及运营主体资料。

正式发布链路已经确定为：GitHub `main` 分支 -> 阿里云 ECS 上的 Jenkins -> 自动测试与构建 -> systemd 发布后端 -> Nginx 发布管理端 -> 同机 MySQL。GitHub、Jenkins、Nginx 和 MySQL 基础环境已完成，服务器仍未完成第一次业务发布。

完整执行状态和中断恢复入口见 [线上部署执行记录](./线上部署执行记录.md)。

## 2. 已登记的阿里云服务器

| 项目 | 当前值 |
| --- | --- |
| 实例 | `iZ2ze4fcoylb8edj1um1isZ` / `i-2ze4fcoylb8edj1um1is` |
| 地域 | 华北 2（北京）/ F |
| 公网 IP | `123.56.169.70` |
| 私网 IP | `172.24.3.178` |
| 规格 | 2 vCPU / 4 GiB / 60 GiB ESSD |
| 镜像 | Alibaba Cloud Linux 3.2104 U13.4，x86_64 |
| 到期时间 | 2026-12-19 23:59:59 |
| 当前核验状态 | 实例运行中；基础环境、Jenkins、Nginx、MySQL 和 4 GiB Swap 已完成 |

当前物理内存约 3.5 GiB，可用 4 GiB Swap 已配置。Jenkins 保持单执行器，流水线继续限制 Maven、Node 和 Java 堆，避免构建与 MySQL、Nginx、后端进程相互挤占。

2026-09-29 已成功执行 `deploy/scripts/prepare-server.sh`，部署账号、目录、systemd 和 sudoers 已落地；随后为加载新增用户组执行 Jenkins 重启时失败。不要重复运行安装流程，先按 systemd 日志定位启动失败原因。

## 3. 正确网络结构

```text
微信小程序
  -> HTTPS 443 / https://api.<域名>/api/v1
  -> Nginx
  -> 127.0.0.1:8081 Spring Boot
  -> 127.0.0.1:3306 / soft_energy_prod

管理员浏览器
  -> HTTPS 443 / https://admin.<域名>
  -> Nginx 静态文件 + /api 反向代理
```

安全组只开放：

- `22`：只允许项目负责人当前固定公网 IP；
- `80`：公网，用于跳转 HTTPS 和证书验证；
- `443`：公网，正式管理端与 API；
- 不开放 `8080`、`8081`、`18080`、`3306`。

Jenkins 只监听 `127.0.0.1:8080`。初始化阶段由 Nginx 暂时代理为 `http://123.56.169.70/jenkins/`；域名证书就绪后必须切换 HTTPS，并限制管理入口访问范围。安全组不开放 8080。

## 4. 第一步：阿里云控制台准备

### 4.1 ECS

1. 当前实例已确认运行，公网和私网 IP 见第 2 节；
2. 在第一次业务发布前创建一次系统盘手动快照；
3. 开启自动续费并设置到期提醒，避免正式服务到期停机；
4. 安全组保留 80/443，部署完成后删除无用 3389，并把 22 收紧到管理员固定出口 IP；
5. 保持 4 GiB Swap 和 Jenkins 单执行器。

### 4.2 同机 MySQL

当前没有购买 RDS，已在 ECS 安装 MySQL 8.0.46：

1. 数据库：`soft_energy_prod`；
2. 字符集：`utf8mb4`；排序规则：`utf8mb4_0900_ai_ci`；
3. 应用账号：`softenergy_app`，只授权 `soft_energy_prod`；
4. 只监听 `127.0.0.1:3306`，安全组不开放 3306；
5. MySQL X 端口 33060 已关闭；
6. root 和应用凭据分别保存在服务器 `/root/.my.cnf` 与 `/root/soft-energy-db.env`，权限均为 600。

应用账号已实际登录验证成功。首次发布前还要配置自动备份并验证恢复；业务量、可用性或运维要求提高后，再迁移到同地域 RDS。

### 4.3 域名与证书

准备两个已备案域名并解析到 `123.56.169.70`：

```text
admin.<你的域名>  -> 123.56.169.70
api.<你的域名>    -> 123.56.169.70
```

申请对应 HTTPS 证书。微信公众平台的 `request` 合法域名填写 `https://api.<你的域名>`，不能带 `/api/v1`，不能使用 IP 或 HTTP。

## 5. 第二步：在服务器终端安装运行环境

先在宝塔面板终端或阿里云“远程连接”中执行：

```bash
cat /etc/os-release
free -h
df -h
```

### 5.1 创建 4 GiB 交换空间

确认 `/swapfile` 不存在后执行：

```bash
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
grep -q '^/swapfile ' /etc/fstab || echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
free -h
```

### 5.2 安装基础软件

宝塔 Linux/Alibaba Cloud Linux 属于 RPM 系发行版时执行：

```bash
sudo dnf install -y git maven curl wget fontconfig java-21-openjdk-devel
java -version
mvn -version
git --version
if [ -x /www/server/nginx/sbin/nginx ]; then /www/server/nginx/sbin/nginx -v; else /usr/sbin/nginx -v; fi
```

通过宝塔 Node.js 版本管理器安装 Node.js 22 LTS，并保证是系统级命令；然后执行：

```bash
node --version
npm --version
sudo corepack enable
sudo corepack prepare pnpm@11.9.0 --activate
pnpm --version
```

要求：Java 21、Node.js 20 以上、pnpm 11.9.0。若宝塔安装路径只对 root 可见，Jenkins 会找不到命令，需要把可执行文件安装到系统 PATH。

### 5.3 安装 Jenkins LTS

按照 Jenkins 官方 RPM 仓库安装：

```bash
sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/rpm-stable/jenkins.repo
sudo dnf install -y jenkins
sudo systemctl daemon-reload
sudo systemctl edit jenkins
```

在编辑器中写入：

```ini
[Service]
Environment="JENKINS_PORT=8080"
Environment="JENKINS_LISTEN_ADDRESS=127.0.0.1"
Environment="JAVA_OPTS=-Xms128m -Xmx384m -XX:+UseG1GC -Djava.awt.headless=true"
```

然后执行：

```bash
sudo systemctl enable --now jenkins
sudo systemctl status jenkins --no-pager
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

不要在阿里云安全组开放 8080。

## 6. 第三步：进入 Jenkins

当前初始化入口：

```text
http://123.56.169.70/jenkins/
```

Jenkins 初始化向导已经完成，管理员用户已经创建。推荐插件主体已经安装，并已确认以下部署插件：

- Pipeline；
- Git；
- GitHub Branch Source；
- Credentials Binding；
- Pipeline: Stage View。

不要共享 Jenkins 密码。当前入口为临时 HTTP 管理入口，域名和 TLS 证书就绪后切换 HTTPS。

## 7. 第四步：GitHub 仓库

2026-09-28 已完成：

- GitHub 账号：`deming1319-bot`；
- 公开仓库：`https://github.com/deming1319-bot/soft-energy-leaderboard`；
- 默认分支：`main`；
- 首次推送根提交：`4cedd42`；
- 仓库包含 `backend`、`admin-web`、`miniapp`、`deploy` 和根目录 `Jenkinsfile`。

Jenkins 尚未接入仓库。当前仓库为公开仓库，首次接入可使用只读 HTTPS 地址而不保存 GitHub 密码或个人令牌：

```text
https://github.com/deming1319-bot/soft-energy-leaderboard.git
```

后续如改为私有仓库，再为 Jenkins 配置仅访问本仓库的 GitHub App 凭据。不要共享个人 GitHub 密码。

## 8. 第五步：准备服务器目录与生产配置

GitHub 仓库完成后，在服务器临时克隆仓库并执行：

```bash
git clone https://github.com/deming1319-bot/soft-energy-leaderboard.git /tmp/soft-energy-bootstrap
cd /tmp/soft-energy-bootstrap
sudo bash deploy/scripts/prepare-server.sh
```

脚本会：

- 创建 `softenergy` 低权限服务用户；
- 创建 `/opt/soft-energy/shared`、`releases`；
- 安装 systemd 服务文件；
- 安装 Jenkins 最小 sudo 权限；
- 复制生产配置模板，但不会填写任何真实密钥。

编辑：

```bash
sudo vi /opt/soft-energy/shared/soft-energy.env
sudo vi /opt/soft-energy/shared/application-prod.yml
```

`soft-energy.env` 必须替换：

- `DB_URL`：服务器现有 `/root/soft-energy-db.env` 中的本机 MySQL 地址；
- `DB_USERNAME`、`DB_PASSWORD`；
- `JWT_SECRET_BASE64`、`PHONE_KEY_BASE64`、`PHONE_HMAC_BASE64`；
- `WECHAT_APP_ID`、`WECHAT_APP_SECRET`；
- `ADMIN_ORIGIN=https://admin.<你的域名>`；
- 管理员正式账号和强密码。

三个随机密钥分别执行一次生成，不要复用：

```bash
openssl rand -base64 32
```

配置权限：

```bash
sudo chown root:softenergy /opt/soft-energy/shared/application-prod.yml /opt/soft-energy/shared/soft-energy.env
sudo chmod 640 /opt/soft-energy/shared/application-prod.yml /opt/soft-energy/shared/soft-energy.env
sudo systemctl restart jenkins
```

## 9. 第六步：配置 Nginx 与 HTTPS

把仓库中的 `deploy/nginx/soft-energy.conf` 复制为服务器配置前，必须替换：

- `admin.example.com`；
- `api.example.com`；
- 两套证书 `.pem` 和 `.key` 路径。

宝塔 Nginx 常用站点配置目录是 `/www/server/panel/vhost/nginx/`；系统 Nginx 常用 `/etc/nginx/conf.d/`。以 `nginx -t` 输出的实际主配置为准，不要同时安装两份重复配置。

安装后执行：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 10. 第七步：Jenkins 控制台配置

### 10.1 GitHub 访问

当前仓库为公开仓库，Jenkins 拉取代码时不需要 GitHub 凭据，直接填写仓库 HTTPS 地址即可。若以后改为私有仓库，再创建只安装到本项目仓库、只拥有 Contents Read-only 和 Metadata Read-only 权限的 GitHub App；私钥只能保存到 Jenkins Credentials，不能提交到代码仓库。

### 10.2 创建流水线

1. 点击“新建任务”；
2. 名称：`soft-energy-leaderboard`；
3. 选择“多分支流水线”；
4. Branch Sources 选择 Git；
5. Project Repository 填写 `https://github.com/deming1319-bot/soft-energy-leaderboard.git`；
6. Credentials 选择 `- none -`；
7. Build Configuration 使用仓库根目录 `Jenkinsfile`；
8. 扫描触发器设置“若没有运行则定期扫描”，间隔 5 分钟；
9. 保存并执行“立即扫描多分支流水线”。

Jenkins 官方说明，多分支流水线会为仓库中包含 Jenkinsfile 的分支自动创建任务，并提供 `BRANCH_NAME` 和 `checkout scm`；本项目 Jenkinsfile 已按此方式配置。

## 11. 第八步：第一次构建与发布

第一次只验证，不部署：

1. 打开 `main` 任务；
2. 点击“Build with Parameters”；
3. `DEPLOY_TO_SERVER=false`；
4. `RUN_MINIAPP_RELEASE_GATE=false`；
5. 宝塔 Nginx 保持默认 `NGINX_BIN=/www/server/nginx/sbin/nginx`；若实际使用系统 Nginx，改成 `/usr/sbin/nginx`；
6. 执行构建，确认三个质量阶段全部通过。

第一次正式发布：

1. 确认 MySQL、生产配置和 Nginx 已完成；公网 IP 验证阶段可暂不配置正式证书；
2. 再次打开 `main -> Build with Parameters`；
3. 勾选 `DEPLOY_TO_SERVER=true`；
4. 小程序正式域名和主体资料没补齐前仍保持 `RUN_MINIAPP_RELEASE_GATE=false`；
5. 点击构建。

成功后服务器应存在：

```text
/opt/soft-energy/releases/<构建号>/app/soft-energy-api.jar
/opt/soft-energy/releases/<构建号>/web/index.html
/opt/soft-energy/current -> 对应构建目录
```

## 12. 发布后检查

服务器终端：

```bash
sudo systemctl status soft-energy-api --no-pager
curl -fsS http://127.0.0.1:8081/api/v1/health
curl -fsS http://127.0.0.1:18080/api/v1/health
curl -I https://admin.<你的域名>/
curl -fsS https://api.<你的域名>/api/v1/health
sudo journalctl -u soft-energy-api -n 100 --no-pager
```

预期健康响应包含 `"status":"UP"`。随后浏览器登录管理后台，创建一条测试题，但在正式运营前清理测试题和测试答题数据。

## 13. 小程序连接线上后端

正式 API 域名确定后，只需要把 `miniapp/miniprogram/config/runtime.ts` 中：

```ts
const PRODUCTION_API_BASE_URL = 'https://api.example.com/api/v1'
```

替换为真实域名。不要填写 `:8081`，也不要填写数据库地址。小程序只访问 Nginx 的 HTTPS 443。

同时完成：

1. 确认 `miniapp/project.config.json` 中现有正式 AppID 与微信公众平台一致；
2. 微信公众平台配置 `request` 合法域名；
3. 服务器 `WECHAT_APP_ID` 和 `WECHAT_APP_SECRET` 与小程序一致；
4. 补齐运营主体、隐私联系人、地址和备案号；
5. 本地执行 `cd miniapp && pnpm verify:release`；
6. 全部通过后，Jenkins 才勾选 `RUN_MINIAPP_RELEASE_GATE=true`。

小程序上传体验版、提交审核和正式发布仍需要在微信开发者工具/微信公众平台人工确认，不能由 Jenkins 无确认地直接正式发布。

## 14. 当前还需要项目负责人提供

- 正式域名及备案状态；
- 与项目现有微信 AppID 对应的 AppSecret（只在服务器填写）；
- 运营主体、隐私联系人、联系地址和小程序备案号；
- 管理员正式初始账号及强密码。

## 15. 官方依据

- [Jenkins Linux 安装与 Java 21 要求](https://www.jenkins.io/doc/book/installing/linux/)
- [Jenkins 多分支流水线](https://www.jenkins.io/doc/book/pipeline/multibranch/)
- [Jenkins Pipeline as Code](https://www.jenkins.io/doc/book/pipeline/pipeline-as-code/)
- [微信小程序 CI](https://developers.weixin.qq.com/miniprogram/dev/devtools/ci.html)
