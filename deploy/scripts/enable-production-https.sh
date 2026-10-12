#!/usr/bin/env bash
set -Eeuo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "请使用 root 执行本脚本。" >&2
  exit 1
fi

for command_name in awk curl grep install openssl rm rmdir sha256sum ss systemctl unzip wc; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "缺少命令：${command_name}。" >&2
    exit 1
  fi
done

NGINX_BIN="${NGINX_BIN:-/usr/sbin/nginx}"
ROOT_ZIP="${ROOT_ZIP:-/root/soft-energy-cert-upload/27793373_ruannengliang.top_nginx.zip}"
API_ZIP="${API_ZIP:-/root/soft-energy-cert-upload/27793399_api.ruannengliang.top_nginx.zip}"
ROOT_ZIP_SHA256='94704504fa9967af6bfbcc752fe28d5da25732e32b66df4b786778f527d18cb4'
API_ZIP_SHA256='080c00e7e5568b8b54661f00bafa9f2366a67072ab2441b684644f202c659d2a'

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_CONFIG="${SOURCE_CONFIG:-${SCRIPT_DIR}/../nginx/soft-energy.conf}"
CURRENT_CONFIG='/etc/nginx/conf.d/jenkins-ip.conf'
TARGET_CONFIG='/etc/nginx/conf.d/soft-energy.conf'
ROOT_CERT_DIR='/etc/nginx/ssl/ruannengliang.top'
API_CERT_DIR='/etc/nginx/ssl/api.ruannengliang.top'
BACKUP_DIR="/root/nginx-https-backups/$(date +%Y%m%d-%H%M%S)"
TEMP_DIR="$(mktemp -d /tmp/soft-energy-https.XXXXXX)"
INSTALL_STARTED=false
COMPLETED=false

cleanup_temp() {
  case "${TEMP_DIR}" in
    /tmp/soft-energy-https.*)
      rm -rf -- "${TEMP_DIR}"
      ;;
    *)
      echo "拒绝清理异常临时路径：${TEMP_DIR}" >&2
      ;;
  esac
}

rollback() {
  local original_exit="$?"
  trap - EXIT

  if [[ "${INSTALL_STARTED}" == 'true' && "${COMPLETED}" != 'true' ]]; then
    echo 'HTTPS 切换未全部通过，正在恢复原 Nginx 配置。' >&2

    rm -f -- "${TARGET_CONFIG}"
    if [[ -f "${BACKUP_DIR}/jenkins-ip.conf" ]]; then
      install -o root -g root -m 0644 \
        "${BACKUP_DIR}/jenkins-ip.conf" "${CURRENT_CONFIG}"
    fi

    rm -f -- \
      "${ROOT_CERT_DIR}/ruannengliang.top.pem" \
      "${ROOT_CERT_DIR}/ruannengliang.top.key" \
      "${API_CERT_DIR}/api.ruannengliang.top.pem" \
      "${API_CERT_DIR}/api.ruannengliang.top.key"
    rmdir -- "${ROOT_CERT_DIR}" "${API_CERT_DIR}" 2>/dev/null || true

    if "${NGINX_BIN}" -t; then
      systemctl reload nginx || true
      echo '原 Nginx 配置已恢复。' >&2
    else
      echo '恢复后的 Nginx 配置校验失败，请立即检查备份目录。' >&2
    fi
  fi

  cleanup_temp
  exit "${original_exit}"
}

trap rollback EXIT

printf '\n[1/7] 检查输入和当前状态\n'

test -x "${NGINX_BIN}"
test -f "${ROOT_ZIP}"
test -f "${API_ZIP}"
test -f "${SOURCE_CONFIG}"
test -f "${CURRENT_CONFIG}"

if [[ -e "${TARGET_CONFIG}" || -e "${ROOT_CERT_DIR}" || -e "${API_CERT_DIR}" ]]; then
  echo '检测到正式配置或证书目录已存在。为避免覆盖现有 HTTPS 配置，本次停止。' >&2
  exit 1
fi

systemctl is-active --quiet nginx
systemctl is-active --quiet jenkins
systemctl is-active --quiet soft-energy-api

printf '\n[2/7] 复核证书压缩包完整性\n'

printf '%s  %s\n' "${ROOT_ZIP_SHA256}" "${ROOT_ZIP}" | sha256sum -c -
printf '%s  %s\n' "${API_ZIP_SHA256}" "${API_ZIP}" | sha256sum -c -

test "$(unzip -Z1 "${ROOT_ZIP}" | wc -l)" -eq 2
unzip -Z1 "${ROOT_ZIP}" | grep -Fxq 'ruannengliang.top.pem'
unzip -Z1 "${ROOT_ZIP}" | grep -Fxq 'ruannengliang.top.key'
test "$(unzip -Z1 "${API_ZIP}" | wc -l)" -eq 2
unzip -Z1 "${API_ZIP}" | grep -Fxq 'api.ruannengliang.top.pem'
unzip -Z1 "${API_ZIP}" | grep -Fxq 'api.ruannengliang.top.key'

umask 077
unzip -p "${ROOT_ZIP}" ruannengliang.top.pem >"${TEMP_DIR}/ruannengliang.top.pem"
unzip -p "${ROOT_ZIP}" ruannengliang.top.key >"${TEMP_DIR}/ruannengliang.top.key"
unzip -p "${API_ZIP}" api.ruannengliang.top.pem >"${TEMP_DIR}/api.ruannengliang.top.pem"
unzip -p "${API_ZIP}" api.ruannengliang.top.key >"${TEMP_DIR}/api.ruannengliang.top.key"

printf '\n[3/7] 验证券域名、有效期和私钥匹配\n'

openssl x509 -in "${TEMP_DIR}/ruannengliang.top.pem" -noout -checkhost ruannengliang.top >/dev/null
openssl x509 -in "${TEMP_DIR}/api.ruannengliang.top.pem" -noout -checkhost api.ruannengliang.top >/dev/null
openssl x509 -in "${TEMP_DIR}/ruannengliang.top.pem" -noout -checkend 604800 >/dev/null
openssl x509 -in "${TEMP_DIR}/api.ruannengliang.top.pem" -noout -checkend 604800 >/dev/null
openssl pkey -in "${TEMP_DIR}/ruannengliang.top.key" -noout >/dev/null
openssl pkey -in "${TEMP_DIR}/api.ruannengliang.top.key" -noout >/dev/null

root_cert_hash="$(
  openssl x509 -in "${TEMP_DIR}/ruannengliang.top.pem" -pubkey -noout |
    openssl pkey -pubin -outform DER 2>/dev/null |
    sha256sum |
    awk '{print $1}'
)"
root_key_hash="$(
  openssl pkey -in "${TEMP_DIR}/ruannengliang.top.key" -pubout 2>/dev/null |
    openssl pkey -pubin -outform DER 2>/dev/null |
    sha256sum |
    awk '{print $1}'
)"
api_cert_hash="$(
  openssl x509 -in "${TEMP_DIR}/api.ruannengliang.top.pem" -pubkey -noout |
    openssl pkey -pubin -outform DER 2>/dev/null |
    sha256sum |
    awk '{print $1}'
)"
api_key_hash="$(
  openssl pkey -in "${TEMP_DIR}/api.ruannengliang.top.key" -pubout 2>/dev/null |
    openssl pkey -pubin -outform DER 2>/dev/null |
    sha256sum |
    awk '{print $1}'
)"

test "${root_cert_hash}" = "${root_key_hash}"
test "${api_cert_hash}" = "${api_key_hash}"
echo '两张证书的域名、剩余有效期和私钥匹配检查均通过。'

printf '\n[4/7] 备份当前 Nginx 配置\n'

install -d -o root -g root -m 0700 "${BACKUP_DIR}"
cp -a -- "${CURRENT_CONFIG}" "${BACKUP_DIR}/jenkins-ip.conf"
printf '备份目录：%s\n' "${BACKUP_DIR}"

INSTALL_STARTED=true

printf '\n[5/7] 安装证书和正式配置\n'

install -d -o root -g root -m 0755 /etc/nginx/ssl
install -d -o root -g root -m 0700 "${ROOT_CERT_DIR}" "${API_CERT_DIR}"
install -o root -g root -m 0644 \
  "${TEMP_DIR}/ruannengliang.top.pem" "${ROOT_CERT_DIR}/ruannengliang.top.pem"
install -o root -g root -m 0600 \
  "${TEMP_DIR}/ruannengliang.top.key" "${ROOT_CERT_DIR}/ruannengliang.top.key"
install -o root -g root -m 0644 \
  "${TEMP_DIR}/api.ruannengliang.top.pem" "${API_CERT_DIR}/api.ruannengliang.top.pem"
install -o root -g root -m 0600 \
  "${TEMP_DIR}/api.ruannengliang.top.key" "${API_CERT_DIR}/api.ruannengliang.top.key"
install -o root -g root -m 0644 "${SOURCE_CONFIG}" "${TARGET_CONFIG}"
rm -f -- "${CURRENT_CONFIG}"

printf '\n[6/7] 校验并重新加载 Nginx\n'

"${NGINX_BIN}" -t
systemctl reload nginx
systemctl is-active --quiet nginx

printf '\n[7/7] 验证管理端、API、Jenkins 和 HTTP 跳转\n'

curl -fsS \
  --resolve 'ruannengliang.top:443:127.0.0.1' \
  https://ruannengliang.top/ \
  -o "${TEMP_DIR}/admin.html"
grep -Eq '<div[^>]+id="app"' "${TEMP_DIR}/admin.html"

curl -fsS \
  --resolve 'api.ruannengliang.top:443:127.0.0.1' \
  https://api.ruannengliang.top/api/v1/health \
  -o "${TEMP_DIR}/health.json"
grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' "${TEMP_DIR}/health.json"

curl -fsS \
  --resolve 'ruannengliang.top:443:127.0.0.1' \
  https://ruannengliang.top/jenkins/login \
  -o /dev/null

redirect_result="$(
  curl -sS \
    --resolve 'ruannengliang.top:80:127.0.0.1' \
    -o /dev/null \
    -w '%{http_code}|%{redirect_url}' \
    http://ruannengliang.top/
)"
test "${redirect_result}" = '301|https://ruannengliang.top/'

ss -lnt | grep -Eq '[:.]443[[:space:]]'

COMPLETED=true
printf '\nHTTPS 切换完成。\n'
printf '管理端：https://ruannengliang.top/\n'
printf 'API 健康检查：https://api.ruannengliang.top/api/v1/health\n'
printf 'Jenkins：https://ruannengliang.top/jenkins/\n'
printf '回滚备份：%s\n' "${BACKUP_DIR}"
