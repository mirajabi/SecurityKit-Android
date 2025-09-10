# Cryptography

## Hashing / HMAC
- SHA-256/384/512
- Constant-time equals
- HMAC-SHA256 (used by signed config and tamper evidence)

## Symmetric crypto
- AES-GCM (recommended), AES-CBC (legacy interop)
- Android Keystore backed keys by default

## Asymmetric crypto
- RSA-OAEP utilities

## EncryptedPreferences
- Simple helper for secure key-value storage
