#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "请使用 root 或 sudo 执行本脚本。" >&2
  exit 1
fi

for command_name in java git mvn node pnpm curl systemctl visudo; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "缺少命令：${command_name}。请先按部署手册安装服务器运行环境。" >&2
    exit 1
  fi
done

if [[ ! -x /www/server/nginx/sbin/nginx && ! -x /usr/sbin/nginx ]]; then
  echo "没有找到宝塔或系统 Nginx，请先安装 Nginx。" >&2
  exit 1
fi

java -version 2>&1 | grep -q 'version "21' || {
  echo "服务器必须使用 Java 21。" >&2
  exit 1
}

getent group softenergy >/dev/null 2>&1 || groupadd --system softenergy
if ! id softenergy >/dev/null 2>&1; then
  useradd --system --gid softenergy --home-dir /opt/soft-energy --shell /sbin/nologin softenergy
fi

if ! id jenkins >/dev/null 2>&1; then
  echo "尚未找到 jenkins 用户，请先安装并启动 Jenkins。" >&2
  exit 1
fi
usermod -aG softenergy jenkins

install -d -o jenkins -g softenergy -m 2775 /opt/soft-energy
install -d -o jenkins -g softenergy -m 2775 /opt/soft-energy/releases
install -d -o root -g softenergy -m 0750 /opt/soft-energy/shared

if [[ ! -f /opt/soft-energy/shared/application-prod.yml ]]; then
  install -o root -g softenergy -m 0640 deploy/config/application-prod.example.yml \
    /opt/soft-energy/shared/application-prod.yml
fi
if [[ ! -f /opt/soft-energy/shared/soft-energy.env ]]; then
  install -o root -g softenergy -m 0640 deploy/config/soft-energy.env.example \
    /opt/soft-energy/shared/soft-energy.env
fi

install -o root -g root -m 0644 deploy/systemd/soft-energy-api.service \
  /etc/systemd/system/soft-energy-api.service
install -o root -g root -m 0440 deploy/sudoers/soft-energy-jenkins \
  /etc/sudoers.d/soft-energy-jenkins
visudo -cf /etc/sudoers.d/soft-energy-jenkins
systemctl daemon-reload

echo "服务器目录、systemd 和 Jenkins 最小 sudo 权限已准备。"
echo "下一步：编辑 /opt/soft-energy/shared 下两个配置文件，并安装已替换域名和证书路径的 Nginx 配置。"
echo "修改 jenkins 用户组后需要重启 Jenkins：systemctl restart jenkins"
