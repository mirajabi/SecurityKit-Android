#!/usr/bin/env python3
"""
Sign a SecurityModule JSON config with HMAC-SHA256 (RAW JSON bytes).

Usage examples:
  macOS/Linux:
    CONFIG_HMAC_KEY="mysecret" ./scripts/sign-config-hmac.sh \
      --config security-sample/src/main/assets/security_config.json \
      --key-env CONFIG_HMAC_KEY --out security-sample/src/main/assets/security_config.sig

  Windows PowerShell:
    $env:CONFIG_HMAC_KEY="mysecret"; \
    py scripts/sign_config_hmac.py --config security-sample\src\main\assets\security_config.json \
      --key-env CONFIG_HMAC_KEY --out security-sample\src\main\assets\security_config.sig
"""
import argparse
import hashlib
import hmac
import os
import sys


def hmac_sha256_hex(data: bytes, key: bytes) -> str:
    return hmac.new(key, data, hashlib.sha256).hexdigest()


def main():
    parser = argparse.ArgumentParser(description="Sign SecurityModule config with HMAC-SHA256 (RAW JSON)")
    parser.add_argument("--config", required=True, help="Path to security_config.json")
    g = parser.add_mutually_exclusive_group(required=True)
    g.add_argument("--key", help="HMAC key as a literal string")
    g.add_argument("--key-env", dest="key_env", help="Environment variable name that contains the key")
    g.add_argument("--key-file", dest="key_file", help="Path to a file containing the key")
    parser.add_argument("--out", default=None, help="Path to write signature (hex). If omitted, prints to stdout")
    args = parser.parse_args()

    # Load key
    if args.key is not None:
        key = args.key.encode("utf-8")
    elif args.key_env is not None:
        val = os.environ.get(args.key_env)
        if not val:
            print(f"Environment variable {args.key_env} is empty or not set", file=sys.stderr)
            sys.exit(2)
        key = val.encode("utf-8")
    else:
        with open(args.key_file, "rb") as f:
            key = f.read().strip()

    # Load config RAW
    with open(args.config, "rb") as f:
        raw = f.read()

    sig = hmac_sha256_hex(raw, key)

    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            f.write(sig)
        print(args.out)
    else:
        print(sig)


if __name__ == "__main__":
    main()


