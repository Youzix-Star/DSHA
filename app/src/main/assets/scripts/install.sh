#!/data/data/com.termux/files/usr/bin/bash
# 安装 / 更新 DSHA 运行环境：克隆安装脚本仓库并执行 setup.sh
set -uo pipefail

REPO_DIR="$HOME/deepseek-harness-android"
REPO_URL="https://github.com/FunnelCakes/deepseek-harness-android.git"
MIRROR_URL="https://gitclone.com/github.com/FunnelCakes/deepseek-harness-android.git"

info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[!]\033[0m %s\n' "$*"; }

command -v git >/dev/null 2>&1 || {
  info "安装 git ..."
  pkg install -y git || { warn "git 安装失败，请检查网络"; exit 1; }
}

if [ -d "$REPO_DIR/.git" ]; then
  info "更新安装脚本仓库 ..."
  git -C "$REPO_DIR" pull --ff-only || warn "更新失败，改用本地已有副本继续"
else
  info "克隆安装脚本仓库 ..."
  if ! timeout 180 git clone --depth 1 "$REPO_URL" "$REPO_DIR"; then
    warn "直连 GitHub 失败或超时，改用镜像重试 ..."
    rm -rf "$REPO_DIR"
    timeout 180 git clone --depth 1 "$MIRROR_URL" "$REPO_DIR" || {
      warn "镜像也失败，请开启代理/TUN 后重试"
      exit 1
    }
  fi
fi

[ -f "$REPO_DIR/setup.sh" ] || { warn "未找到 setup.sh，仓库内容异常"; exit 1; }

info "开始执行 setup.sh（首次安装需下载依赖并做原生编译，约 5~15 分钟）"
cd "$REPO_DIR"
bash "$REPO_DIR/setup.sh"
