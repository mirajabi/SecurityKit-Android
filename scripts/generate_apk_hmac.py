#!/usr/bin/env python3
"""
APK HMAC Signature Generator

This script generates HMAC signatures for APK files to prevent repackaging.
It should be run during the build process to create secure signatures.

Usage:
    python3 generate_apk_hmac.py <apk_file_path> [output_file_path]

Example:
    python3 generate_apk_hmac.py app-release.apk apk_hmac_signature.txt
"""

import hashlib
import hmac
import os
import sys
import json
import time
from pathlib import Path

def generate_secure_key():
    """Generate a secure HMAC key"""
    # In production, this should use a secure key derivation function
    # For demonstration, we'll use a combination of system info and random data
    import secrets
    import platform
    
    # Create a device-specific key (simulated)
    device_info = f"{platform.system()}_{platform.machine()}_{platform.node()}"
    random_data = secrets.token_hex(32)
    
    # Combine and hash to create a secure key
    combined = f"{device_info}_{random_data}_{int(time.time())}"
    key = hashlib.sha256(combined.encode()).digest()
    
    return key

def calculate_apk_hash(apk_path):
    """Calculate SHA-256 hash of APK file"""
    sha256_hash = hashlib.sha256()
    
    with open(apk_path, "rb") as f:
        # Read file in chunks to handle large APK files
        for chunk in iter(lambda: f.read(8192), b""):
            sha256_hash.update(chunk)
    
    return sha256_hash.hexdigest()

def generate_hmac_signature(apk_path, key):
    """Generate HMAC-SHA256 signature for APK"""
    # Calculate APK hash first
    apk_hash = calculate_apk_hash(apk_path)
    
    # Generate HMAC signature
    hmac_signature = hmac.new(
        key,
        apk_hash.encode('utf-8'),
        hashlib.sha256
    ).hexdigest()
    
    return hmac_signature, apk_hash

def create_signature_data(apk_path, hmac_signature, apk_hash, key_type="software"):
    """Create comprehensive signature data"""
    apk_file = Path(apk_path)
    
    return {
        "apk_file": apk_file.name,
        "apk_path": str(apk_file.absolute()),
        "apk_hash": apk_hash,
        "hmac_signature": hmac_signature,
        "key_type": key_type,
        "timestamp": int(time.time()),
        "algorithm": "HMAC-SHA256",
        "hash_algorithm": "SHA-256",
        "version": "1.0.0"
    }

def save_signature_data(signature_data, output_path):
    """Save signature data to file"""
    with open(output_path, 'w') as f:
        json.dump(signature_data, f, indent=2)
    
    print(f"✅ Signature data saved to: {output_path}")

def save_signature_only(hmac_signature, output_path):
    """Save only the HMAC signature to file"""
    with open(output_path, 'w') as f:
        f.write(hmac_signature)
    
    print(f"✅ HMAC signature saved to: {output_path}")

def main():
    if len(sys.argv) < 2:
        print("❌ Error: APK file path is required")
        print("Usage: python3 generate_apk_hmac.py <apk_file_path> [output_file_path]")
        sys.exit(1)
    
    apk_path = sys.argv[1]
    
    # Check if APK file exists
    if not os.path.exists(apk_path):
        print(f"❌ Error: APK file not found: {apk_path}")
        sys.exit(1)
    
    # Generate output path if not provided
    if len(sys.argv) >= 3:
        output_path = sys.argv[2]
    else:
        apk_name = Path(apk_path).stem
        output_path = f"{apk_name}_hmac_signature.txt"
    
    print(f"🔐 Generating HMAC signature for APK: {apk_path}")
    print(f"📁 Output file: {output_path}")
    
    try:
        # Generate secure key
        print("🔑 Generating secure HMAC key...")
        key = generate_secure_key()
        print(f"   Key generated: {key.hex()[:16]}...")
        
        # Calculate APK hash
        print("📊 Calculating APK hash...")
        apk_hash = calculate_apk_hash(apk_path)
        print(f"   APK hash: {apk_hash[:16]}...")
        
        # Generate HMAC signature
        print("🔏 Generating HMAC signature...")
        hmac_signature, apk_hash = generate_hmac_signature(apk_path, key)
        print(f"   HMAC signature: {hmac_signature[:16]}...")
        
        # Create signature data
        signature_data = create_signature_data(apk_path, hmac_signature, apk_hash)
        
        # Save signature data
        if output_path.endswith('.json'):
            save_signature_data(signature_data, output_path)
        else:
            save_signature_only(hmac_signature, output_path)
        
        # Print summary
        print("\n📋 SUMMARY:")
        print(f"   APK File: {Path(apk_path).name}")
        print(f"   APK Size: {os.path.getsize(apk_path) / (1024*1024):.2f} MB")
        print(f"   APK Hash: {apk_hash}")
        print(f"   HMAC Signature: {hmac_signature}")
        print(f"   Key Type: {signature_data['key_type']}")
        print(f"   Timestamp: {signature_data['timestamp']}")
        print(f"   Output File: {output_path}")
        
        print("\n✅ APK HMAC signature generated successfully!")
        print("💡 This signature should be embedded in your app for runtime verification.")
        
    except Exception as e:
        print(f"❌ Error generating HMAC signature: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
