# Best Practices

- Keep integrity fields (signing SHA-256, DEX/so hashes) current per build flavor
- Use signed configuration (HMAC RAW JSON) for production
- Restrict device overrides to controlled environments (QA/emulators)
- Prefer AES-GCM with Android Keystore; enable StrongBox when available
- Handle Play Integrity gracefully
- Run heavy checks off main thread in complex UIs
