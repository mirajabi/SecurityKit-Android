# Tamper Evidence Store

Store critical data with an attached HMAC (MAC over value + metadata) to detect tampering.

- API supports version and timestamp fields
- Validate on retrieval
- Key rotation logic supported

Use-cases: caching policies/config from server, flags, and sensitive toggles.
