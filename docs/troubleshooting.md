# Troubleshooting

- Signature mismatch: ensure correct keystore; hashes must be lowercase and colon-less
- DEX/so mismatch: regenerate after code/lib changes
- Screenshots on emulator: enable monitor + overlay; FLAG_SECURE alone may be insufficient
- Java 8 compatibility: set module `compileOptions` and Kotlin `jvmTarget` to 1.8
