# Play Integrity (Optional)

SecurityModule accesses Play Integrity via reflection so the library builds without a hard dependency.

- If Google Play Services is unavailable, results are UNAVAILABLE/UNKNOWN and the system falls back gracefully.
- Configure under `advanced.playIntegrity` in your JSON.
- Treat results as additional signals and map to policy.
