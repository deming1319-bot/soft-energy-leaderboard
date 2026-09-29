#!/usr/bin/env bash
set -euo pipefail
set +H
umask 027

if [[ "${EUID}" -ne 0 ]]; then
  echo "请使用 root 或 sudo 执行本脚本。" >&2
  exit 1
fi

DB_SOURCE="/root/soft-energy-db.env"
TARGET="/opt/soft-energy/shared/soft-energy.env"
PROD_CONFIG="/opt/soft-energy/shared/application-prod.yml"
BACKUP_DIR="/root/soft-energy-config-backups"
TEMP_FILE=""

cleanup() {
  if [[ -n "${TEMP_FILE}" && -f "${TEMP_FILE}" ]]; then
    rm -f -- "${TEMP_FILE}"
  fi
  unset DB_PASSWORD WECHAT_APP_SECRET ADMIN_PASSWORD ADMIN_PASSWORD_CONFIRM
}
trap cleanup EXIT

for command_name in mysql openssl sed grep stat install sudo; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "缺少命令：${command_name}" >&2
    exit 1
  fi
done

printf '\n[1/6] 检查数据库和部署目录\n'
test -f "${DB_SOURCE}"
test "$(stat -c '%a' "${DB_SOURCE}")" = "600"
test -f "${PROD_CONFIG}"
test -d "$(dirname "${TARGET}")"

DB_PASSWORD="$(sed -n 's/^DB_PASSWORD=//p' "${DB_SOURCE}" | tail -n 1)"
if ! [[ "${DB_PASSWORD}" =~ ^[0-9a-f]{48}$ ]]; then
  echo "数据库密码文件格式异常，已停止。" >&2
  exit 1
fi

MYSQL_PWD="${DB_PASSWORD}" mysql --no-defaults \
  --protocol=TCP \
  -h127.0.0.1 \
  -P3306 \
  -usoftenergy_app \
  -Dsoft_energy_prod \
  -Nse 'SELECT 1;' >/dev/null
echo "数据库连接验证通过。"

printf '\n[2/6] 输入微信小程序生产凭据\n'
read -r -p '请输入小程序 AppID：' WECHAT_APP_ID
if ! [[ "${WECHAT_APP_ID}" =~ ^wx[[:alnum:]]{16}$ ]]; then
  echo "AppID 格式不正确，应为 wx 开头的 18 位字符串。" >&2
  exit 1
fi

read -r -s -p '请输入小程序 AppSecret（输入内容不会显示）：' WECHAT_APP_SECRET
printf '\n'
if ! [[ "${WECHAT_APP_SECRET}" =~ ^[[:alnum:]]{32}$ ]]; then
  echo "AppSecret 格式不正确，应为 32 位字母或数字。" >&2
  exit 1
fi

printf '\n[3/6] 设置管理后台账号\n'
read -r -p '请输入管理后台用户名，直接回车默认 admin：' ADMIN_USERNAME
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
if ! [[ "${ADMIN_USERNAME}" =~ ^[A-Za-z][A-Za-z0-9_.-]{2,31}$ ]]; then
  echo "用户名必须为 3 至 32 位，并以字母开头。" >&2
  exit 1
fi

read -r -s -p '请输入管理后台密码（输入内容不会显示）：' ADMIN_PASSWORD
printf '\n'
read -r -s -p '请再次输入管理后台密码：' ADMIN_PASSWORD_CONFIRM
printf '\n'

if [[ "${ADMIN_PASSWORD}" != "${ADMIN_PASSWORD_CONFIRM}" ]]; then
  echo "两次输入的管理员密码不一致。" >&2
  exit 1
fi
if ! printf '%s' "${ADMIN_PASSWORD}" | grep -Eq '^[A-Za-z0-9@._%+=:!-]{12,64}$' \
    || ! [[ "${ADMIN_PASSWORD}" =~ [A-Z] ]] \
    || ! [[ "${ADMIN_PASSWORD}" =~ [a-z] ]] \
    || ! [[ "${ADMIN_PASSWORD}" =~ [0-9] ]] \
    || ! printf '%s' "${ADMIN_PASSWORD}" | grep -Eq '[@._%+=:!-]'; then
  echo "密码必须为 12 至 64 位，并包含大小写字母、数字和允许的符号。" >&2
  echo "允许使用的符号：@ . _ % + = : ! -" >&2
  exit 1
fi

printf '\n[4/6] 备份旧配置并生成生产密钥\n'
install -d -o root -g root -m 0700 "${BACKUP_DIR}"
if [[ -f "${TARGET}" ]]; then
  cp -a -- "${TARGET}" \
    "${BACKUP_DIR}/soft-energy.env.$(date +%Y%m%d-%H%M%S).bak"
fi

JWT_SECRET_BASE64="$(openssl rand -base64 48 | tr -d '\n')"
PHONE_KEY_BASE64="$(openssl rand -base64 32 | tr -d '\n')"
PHONE_HMAC_BASE64="$(openssl rand -base64 48 | tr -d '\n')"
TEMP_FILE="$(mktemp /opt/soft-energy/shared/.soft-energy.env.XXXXXX)"

cat >"${TEMP_FILE}" <<EOF
DB_URL=jdbc:mysql://127.0.0.1:3306/soft_energy_prod?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=false&sslMode=DISABLED
DB_USERNAME=softenergy_app
DB_PASSWORD=${DB_PASSWORD}
DB_POOL_MAX=8
DB_POOL_MIN=2
SERVER_PORT=8081
JWT_SECRET_BASE64=${JWT_SECRET_BASE64}
PHONE_KEY_BASE64=${PHONE_KEY_BASE64}
PHONE_HMAC_BASE64=${PHONE_HMAC_BASE64}
WECHAT_APP_ID=${WECHAT_APP_ID}
WECHAT_APP_SECRET=${WECHAT_APP_SECRET}
WECHAT_CONTENT_SECURITY_ENABLED=true
ADMIN_ORIGIN=http://123.56.169.70
ADMIN_USERNAME=${ADMIN_USERNAME}
ADMIN_PASSWORD=${ADMIN_PASSWORD}
ADMIN_DISPLAY_NAME=内容管理员
EOF

chown root:softenergy "${TEMP_FILE}"
chmod 0640 "${TEMP_FILE}"
mv -f -- "${TEMP_FILE}" "${TARGET}"
TEMP_FILE=""

printf '\n[5/6] 验证配置权限与完整性\n'
test "$(stat -c '%a' "${TARGET}")" = "640"
test "$(stat -c '%U:%G' "${TARGET}")" = "root:softenergy"
sudo -u jenkins test -r "${TARGET}"
sudo -u softenergy test -r "${TARGET}"

for config_key in \
  DB_URL DB_USERNAME DB_PASSWORD SERVER_PORT \
  JWT_SECRET_BASE64 PHONE_KEY_BASE64 PHONE_HMAC_BASE64 \
  WECHAT_APP_ID WECHAT_APP_SECRET ADMIN_ORIGIN \
  ADMIN_USERNAME ADMIN_PASSWORD; do
  grep -q "^${config_key}=.\+" "${TARGET}" || {
    echo "缺少配置项：${config_key}" >&2
    exit 1
  }
done

if grep -Eq 'replace-with|your-rds-host|example\.com' "${TARGET}"; then
  echo "配置文件仍包含示例值，已停止。" >&2
  exit 1
fi

printf '\n[6/6] 安全检查结果\n'
stat -c '%a %U:%G %n' "${TARGET}"
echo "数据库连接：正常"
echo "微信生产凭据：已设置"
echo "管理后台账号：已设置"
echo "JWT 与个人信息加密密钥：已随机生成"
echo "所有敏感值均未显示。后端尚未启动。"
