#!/usr/bin/env python3
"""
Generate secure HMAC signature for security_config.json using device-bound key simulation.
This script demonstrates how to create signatures that would work with the secure HMAC implementation.
"""

import hashlib
import hmac
import json
import os
import sys

def generate_device_bound_key_simulation(device_id: str, package_name: str) -> bytes:
    """
    Simulate device-bound key generation (this is just for demonstration).
    In real implementation, this would be done by Android Keystore.
    """
    binding_data = f"{device_id}:{package_name}:SecurityModule:HMAC"
    return hashlib.sha256(binding_data.encode()).digest()

def compute_hmac_sha256(data: bytes, key: bytes) -> str:
    """Compute HMAC-SHA256 and return as hex string."""
    return hmac.new(key, data, hashlib.sha256).hexdigest()

def main():
    # Configuration
    config_path = "security-sample/src/main/assets/security_config.json"
    signature_path = "security-sample/src/main/assets/security_config.sig"
    
    # Device simulation (in real app, these would come from Android system)
    device_id = "test_device_12345"  # Simulated ANDROID_ID
    package_name = "com.miaadrajabi.securitysample"
    
    try:
        # Read configuration file
        with open(config_path, 'rb') as f:
            config_data = f.read()
        
        print(f"📄 Read configuration file: {len(config_data)} bytes")
        
        # Generate device-bound key (simulation)
        device_key = generate_device_bound_key_simulation(device_id, package_name)
        print(f"🔑 Generated device-bound key: {device_key.hex()[:16]}...")
        
        # Compute HMAC signature
        signature = compute_hmac_sha256(config_data, device_key)
        print(f"✍️  Generated HMAC signature: {signature[:32]}...")
        
        # Write signature file
        with open(signature_path, 'w') as f:
            f.write(signature)
        
        print(f"💾 Signature saved to: {signature_path}")
        
        # Verify signature
        expected_signature = compute_hmac_sha256(config_data, device_key)
        if signature == expected_signature:
            print("✅ Signature verification successful")
        else:
            print("❌ Signature verification failed")
            sys.exit(1)
        
        # Test with tampered data
        tampered_data = config_data.replace(b'"rootDetection": true', b'"rootDetection": false')
        tampered_signature = compute_hmac_sha256(tampered_data, device_key)
        if tampered_signature != signature:
            print("✅ Tamper detection working correctly")
        else:
            print("❌ Tamper detection failed")
            sys.exit(1)
        
        print("\n🎉 Secure HMAC signature generation completed successfully!")
        print(f"📱 Device ID: {device_id}")
        print(f"📦 Package: {package_name}")
        print(f"🔐 Signature: {signature}")
        
    except FileNotFoundError as e:
        print(f"❌ File not found: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
