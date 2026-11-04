#!/usr/bin/env bash
# Full API suite trên backend Render (DB Neon thật — CÓ GHI DỮ LIỆU DEMO).
#
# Cách dùng:
#   ./run-render.sh --bail              # hỏi xác nhận rồi chạy full
#   ./run-render.sh --bail --yes        # không hỏi (CI/demo)
#   ./run-render.sh --tags tasks --yes  # chỉ một nhóm
#   RENDER_BASE_URL=... ./run-render.sh --yes
#
# Trước khi chạy, script đánh thức Render (free tier ngủ 15 phút) để request đầu
# không bị timeout 30s.
set -euo pipefail

cd "$(dirname "$0")"
ROOT=$(pwd)
BASE="${RENDER_BASE_URL:-https://remoteapp-backend.onrender.com}"

FORCE=0
PASSTHRU=()
for arg in "$@"; do
  if [[ "$arg" == "--yes" ]]; then
    FORCE=1
  else
    PASSTHRU+=("$arg")
  fi
done

if [[ "$FORCE" != "1" ]]; then
  echo "CẢNH BÁO: bộ test này GHI dữ liệu demo (user/project/task/chat/file)"
  echo "vào backend Render + Neon: $BASE"
  read -r -p "Tiếp tục? [y/N] " answer
  [[ "$answer" =~ ^[Yy]$ ]] || { echo "Đã huỷ."; exit 1; }
fi

echo "Đánh thức $BASE ..."
curl -fsS --retry 20 --retry-delay 3 --retry-all-errors --max-time 120 \
  "$BASE/actuator/health" >/dev/null
echo "Backend UP."

STAMP=$(date +%s)

ARGS=(
  --env-var baseUrl="$BASE"
  --env-var inputEmailA="${USER_A_EMAIL:-demo-a+$STAMP@example.com}"
  --env-var inputEmailB="${USER_B_EMAIL:-demo-b+$STAMP@example.com}"
  --env-var inputEmailC="${USER_C_EMAIL:-demo-c+$STAMP@example.com}"
  --env-var inputShortPwEmail="${SHORT_PW_EMAIL:-demo-short+$STAMP@example.com}"
  --env-var inputPasswordA="${USER_A_PASSWORD:-password-123}"
  --env-var inputPasswordB="${USER_B_PASSWORD:-password-123}"
  --env-var inputPasswordC="${USER_C_PASSWORD:-password-123}"
  --env-var inputNewPasswordA="${USER_A_NEW_PASSWORD:-password-123-v2}"
  --env-var inputFcmToken="${FCM_TOKEN:-fcm-render-$STAMP}"
  --env-var inputUploadFile="${UPLOAD_FILE:-$ROOT/fixtures/sample.txt}"
  --env-var inputAvatarFile="${AVATAR_FILE:-$ROOT/fixtures/sample.png}"
)

cd collections/01-api
exec npx -y @usebruno/cli run -r \
  --env-file ../../environments/render.yml \
  "${ARGS[@]}" \
  "${PASSTHRU[@]}"
