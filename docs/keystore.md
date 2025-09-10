# Keystore (Advanced)

## StrongBox (API 28+)
Prefer StrongBox-backed keys when present.

## User-authentication bound keys
Bind AES keys to biometric/device credential for a validity window (e.g., 5 minutes).

## Device-binding ID
Sign selected device properties with a key pair and hash the result to create a stable identifier for server-side binding.

**Notes**: All helpers gracefully fall back on older API levels.
