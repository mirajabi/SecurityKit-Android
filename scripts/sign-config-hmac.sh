#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   CONFIG_HMAC_KEY="mysecret" ./scripts/sign-config-hmac.sh \
#     --config security-sample/src/main/assets/security_config.json \
#     --out security-sample/src/main/assets/security_config.sig

CONFIG_PATH=""
OUT_PATH=""
KEY="${CONFIG_HMAC_KEY:-}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --config) CONFIG_PATH="$2"; shift 2;;
    --out) OUT_PATH="$2"; shift 2;;
    --key) KEY="$2"; shift 2;;
    *) echo "Unknown arg: $1"; exit 1;;
  esac
done

if [[ -z "$CONFIG_PATH" ]]; then echo "--config is required"; exit 1; fi
if [[ -z "$OUT_PATH" ]]; then echo "--out is required"; exit 1; fi
if [[ -z "$KEY" ]]; then echo "CONFIG_HMAC_KEY env or --key is required"; exit 1; fi

python3 "$(dirname "$0")/sign_config_hmac.py" --config "$CONFIG_PATH" --key "$KEY" --out "$OUT_PATH"
echo "Wrote signature to: $OUT_PATH"


