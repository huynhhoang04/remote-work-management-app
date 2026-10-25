#!/usr/bin/env bash
# Smoke test API bằng Bruno CLI trên môi trường LOCAL (DB dùng một lần).
#
# Cách dùng:
#   ./run.sh                     # tự sinh email riêng cho lần chạy + mật khẩu dev
#   ./run.sh --bail              # dừng ngay khi có request fail
#   USER_A_EMAIL=me@x.com USER_A_PASSWORD=... ./run.sh   # ghi đè giá trị
#
# Script này CHỈ dùng cho CLI/CI: nó truyền giá trị qua --env-var, KHÔNG sửa file environment
# và không ảnh hưởng giá trị bạn đang dùng trong Bruno.
#
# Lưu ý: chỉ trỏ vào local/demo DB (collection có request ghi — xem README mục Safety notes).
set -euo pipefail

cd "$(dirname "$0")"

STAMP=$(date +%s)

ARGS=(
  --env-var inputEmailA="${USER_A_EMAIL:-user-a+$STAMP@example.com}"
  --env-var inputEmailB="${USER_B_EMAIL:-user-b+$STAMP@example.com}"
  --env-var inputShortPwEmail="${SHORT_PW_EMAIL:-short-pw+$STAMP@example.com}"
  --env-var inputPasswordA="${USER_A_PASSWORD:-password-123}"
  --env-var inputPasswordB="${USER_B_PASSWORD:-password-123}"
  --env-var inputNewPasswordA="${USER_A_NEW_PASSWORD:-password-123-v2}"
)

cd collections/01-api
exec npx -y @usebruno/cli run -r \
  --env-file ../../environments/local.yml \
  "${ARGS[@]}" \
  "$@"
