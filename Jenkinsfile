pipeline {
  agent any

  options {
    disableConcurrentBuilds()
    timestamps()
    timeout(time: 45, unit: 'MINUTES')
    buildDiscarder(logRotator(numToKeepStr: '30'))
    skipDefaultCheckout(true)
  }

  parameters {
    string(name: 'DEPLOY_ROOT', defaultValue: '/opt/soft-energy', description: '服务器部署根目录')
    string(name: 'SERVICE_NAME', defaultValue: 'soft-energy-api', description: 'systemd 服务名')
    string(name: 'HEALTH_URL', defaultValue: 'http://127.0.0.1:8081/api/v1/health', description: '后端健康检查地址')
    string(name: 'ADMIN_URL', defaultValue: 'http://127.0.0.1:18080', description: 'Nginx 管理端本机检查地址')
    string(name: 'NGINX_BIN', defaultValue: '/usr/sbin/nginx', description: '当前服务器的系统 Nginx 可执行文件路径')
    booleanParam(name: 'DEPLOY_TO_SERVER', defaultValue: false, description: '仅在 main 或 v* 标签构建时勾选，执行生产发布')
    booleanParam(name: 'RUN_MINIAPP_RELEASE_GATE', defaultValue: false, description: '正式 AppID、域名和主体资料补齐后勾选')
  }

  environment {
    LANG = 'C.UTF-8'
    LC_ALL = 'C.UTF-8'
    CI = 'true'
    NO_COLOR = '1'
    FORCE_COLOR = '0'
    MAVEN_OPTS = '-Xms128m -Xmx384m -XX:+UseG1GC -Djava.awt.headless=true -Dstyle.color=never'
    NODE_OPTIONS = '--max-old-space-size=384'
    JAR_SOURCE = 'backend/target/soft-energy-api-0.1.0-SNAPSHOT.jar'
  }

  stages {
    stage('检出代码') {
      steps {
        deleteDir()
        checkout scm
        sh '''
          set -eu
          git rev-parse --verify HEAD
          git status --short
        '''
      }
    }

    stage('环境校验') {
      steps {
        sh '''
          set -eu
          java -version 2>&1 | grep 'version "21'
          mvn -version
          node --version
          pnpm --version
          command -v curl >/dev/null
          case "$NGINX_BIN" in
            /www/server/nginx/sbin/nginx|/usr/sbin/nginx) ;;
            *) echo "拒绝未授权 Nginx 路径: $NGINX_BIN"; exit 1 ;;
          esac
          test -x "$NGINX_BIN"

          TOTAL_KB=$(awk '/MemTotal|SwapTotal/ { total += $2 } END { print total + 0 }' /proc/meminfo)
          if [ "$TOTAL_KB" -lt 1572864 ]; then
            echo "物理内存与交换空间合计不足 1.5 GiB，请先按部署手册配置交换空间。"
            exit 1
          fi
        '''
      }
    }

    stage('后端测试与构建') {
      steps {
        sh 'cd backend && mvn -B -Dstyle.color=never clean verify'
      }
    }

    stage('管理端测试与构建') {
      steps {
        sh '''
          set -eu
          cd admin-web
          pnpm install --frozen-lockfile
          pnpm test
          pnpm build
        '''
      }
    }

    stage('小程序校验') {
      steps {
        sh '''
          set -eu
          cd miniapp
          pnpm install --frozen-lockfile
          pnpm type-check
          pnpm test
          pnpm build:npm
          test -f miniprogram/miniprogram_npm/tdesign-miniprogram/button/button.js
          if [ "$RUN_MINIAPP_RELEASE_GATE" = "true" ]; then
            pnpm check:release
          else
            echo "正式微信资料尚未补齐，本次跳过小程序正式发布门禁。"
          fi
        '''
      }
    }

    stage('原子发布') {
      when {
        expression { return params.DEPLOY_TO_SERVER }
      }
      steps {
        script {
          def rawDeployRef = env.TAG_NAME ?: env.BRANCH_NAME ?: env.GIT_LOCAL_BRANCH ?: env.GIT_BRANCH
          def deployRef = (rawDeployRef ?: '')
            .replaceFirst('^origin/', '')
            .replaceFirst('^refs/heads/', '')
          if (!(deployRef == 'main' || (deployRef ?: '').startsWith('v'))) {
            error("只允许从 main 或 v* 标签发布，当前来源：${deployRef ?: 'unknown'}")
          }
        }
        sh '''
          set -eu
          umask 0027
          DEPLOY_ROOT="${DEPLOY_ROOT%/}"
          case "$DEPLOY_ROOT" in
            /opt/soft-energy|/srv/soft-energy) ;;
            *) echo "拒绝未授权部署目录: $DEPLOY_ROOT"; exit 1 ;;
          esac

          SHARED_DIR="$DEPLOY_ROOT/shared"
          RELEASE_DIR="$DEPLOY_ROOT/releases/${BUILD_NUMBER}"
          CURRENT_LINK="$DEPLOY_ROOT/current"
          PREVIOUS_RELEASE=""
          if [ -L "$CURRENT_LINK" ]; then PREVIOUS_RELEASE="$(readlink -f "$CURRENT_LINK")"; fi

          test -f "$SHARED_DIR/application-prod.yml" || {
            echo "缺少 $SHARED_DIR/application-prod.yml，请先按 deploy/config 模板配置"
            exit 1
          }
          test -f "$SHARED_DIR/soft-energy.env" || {
            echo "缺少 $SHARED_DIR/soft-energy.env，请先写入生产数据库和微信密钥"
            exit 1
          }
          if grep -Eqi 'replace-with|your-rds-host|example[.]com|__REPLACE_' "$SHARED_DIR/application-prod.yml" "$SHARED_DIR/soft-energy.env"; then
            echo "生产配置仍包含占位值，拒绝发布。"
            exit 1
          fi
          sudo "$NGINX_BIN" -t

          install -d -m 0755 "$RELEASE_DIR" "$RELEASE_DIR/app" "$RELEASE_DIR/web"
          install -m 0644 "$JAR_SOURCE" "$RELEASE_DIR/app/soft-energy-api.jar"
          cp -R admin-web/dist/. "$RELEASE_DIR/web/"
          find "$RELEASE_DIR/web" -type d -exec chmod 0755 {} +
          find "$RELEASE_DIR/web" -type f -exec chmod 0644 {} +
          test -f "$RELEASE_DIR/web/index.html"

          ln -sfn "$RELEASE_DIR" "$DEPLOY_ROOT/current.next"
          mv -Tf "$DEPLOY_ROOT/current.next" "$CURRENT_LINK"
          sudo /usr/bin/systemctl restart "$SERVICE_NAME"

          HEALTHY=false
          for i in $(seq 1 40); do
            if curl -fsS "$HEALTH_URL" | grep -q '"status":"UP"'; then HEALTHY=true; break; fi
            sleep 2
          done

          if [ "$HEALTHY" != true ]; then
            echo "新版本健康检查失败"
            sudo /usr/bin/journalctl -u "$SERVICE_NAME" -n 120 --no-pager || true
            if [ -n "$PREVIOUS_RELEASE" ] && [ -d "$PREVIOUS_RELEASE" ]; then
              ln -sfn "$PREVIOUS_RELEASE" "$DEPLOY_ROOT/current.rollback"
              mv -Tf "$DEPLOY_ROOT/current.rollback" "$CURRENT_LINK"
              sudo /usr/bin/systemctl restart "$SERVICE_NAME"
              echo "已回滚到 $PREVIOUS_RELEASE"
            fi
            exit 1
          fi

          sudo "$NGINX_BIN" -t
          sudo "$NGINX_BIN" -s reload
          if ! curl -fsS "$ADMIN_URL/" | grep -q '<div id="app"></div>'; then
            echo "管理端检查失败"
            if [ -n "$PREVIOUS_RELEASE" ] && [ -d "$PREVIOUS_RELEASE" ]; then
              ln -sfn "$PREVIOUS_RELEASE" "$DEPLOY_ROOT/current.rollback"
              mv -Tf "$DEPLOY_ROOT/current.rollback" "$CURRENT_LINK"
              sudo /usr/bin/systemctl restart "$SERVICE_NAME"
              sudo "$NGINX_BIN" -s reload
              echo "已回滚到 $PREVIOUS_RELEASE"
            fi
            exit 1
          fi
          echo "发布完成: $RELEASE_DIR"
        '''
      }
    }
  }

  post {
    success {
      script {
        if (params.DEPLOY_TO_SERVER) {
          echo '后端、管理端和小程序校验均通过，服务器发布成功。'
        } else {
          echo '后端、管理端和小程序校验均通过；本次仅执行 CI，未发布服务器。'
        }
      }
    }
    failure { echo '流水线失败；若发生在健康检查阶段，脚本已尝试自动回滚。' }
  }
}
