#!/bin/bash

# APK HMAC Signing Script
# This script signs APK files with HMAC signatures to prevent repackaging

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 <apk_file_path> [options]"
    echo ""
    echo "Options:"
    echo "  -o, --output <file>     Output file path for HMAC signature"
    echo "  -a, --assets <dir>      Assets directory to store signature"
    echo "  -v, --verbose           Enable verbose output"
    echo "  -h, --help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 app-release.apk"
    echo "  $0 app-release.apk -o apk_signature.txt"
    echo "  $0 app-release.apk -a src/main/assets/"
    echo ""
}

# Default values
OUTPUT_FILE=""
ASSETS_DIR=""
VERBOSE=false
APK_FILE=""

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -o|--output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        -a|--assets)
            ASSETS_DIR="$2"
            shift 2
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        -*)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
        *)
            if [[ -z "$APK_FILE" ]]; then
                APK_FILE="$1"
            else
                print_error "Multiple APK files specified"
                show_usage
                exit 1
            fi
            shift
            ;;
    esac
done

# Check if APK file is provided
if [[ -z "$APK_FILE" ]]; then
    print_error "APK file path is required"
    show_usage
    exit 1
fi

# Check if APK file exists
if [[ ! -f "$APK_FILE" ]]; then
    print_error "APK file not found: $APK_FILE"
    exit 1
fi

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_SCRIPT="$SCRIPT_DIR/generate_apk_hmac.py"

# Check if Python script exists
if [[ ! -f "$PYTHON_SCRIPT" ]]; then
    print_error "Python script not found: $PYTHON_SCRIPT"
    exit 1
fi

# Generate output file name if not provided
if [[ -z "$OUTPUT_FILE" ]]; then
    APK_NAME=$(basename "$APK_FILE" .apk)
    OUTPUT_FILE="${APK_NAME}_hmac_signature.txt"
fi

print_info "Starting APK HMAC signing process..."
print_info "APK File: $APK_FILE"
print_info "Output File: $OUTPUT_FILE"

# Get APK file size
APK_SIZE=$(du -h "$APK_FILE" | cut -f1)
print_info "APK Size: $APK_SIZE"

# Generate HMAC signature
print_info "Generating HMAC signature..."
if [[ "$VERBOSE" == "true" ]]; then
    python3 "$PYTHON_SCRIPT" "$APK_FILE" "$OUTPUT_FILE"
else
    python3 "$PYTHON_SCRIPT" "$APK_FILE" "$OUTPUT_FILE" > /dev/null 2>&1
fi

# Check if signature was generated successfully
if [[ ! -f "$OUTPUT_FILE" ]]; then
    print_error "Failed to generate HMAC signature"
    exit 1
fi

print_success "HMAC signature generated successfully"

# Copy to assets directory if specified
if [[ -n "$ASSETS_DIR" ]]; then
    if [[ ! -d "$ASSETS_DIR" ]]; then
        print_warning "Assets directory does not exist, creating: $ASSETS_DIR"
        mkdir -p "$ASSETS_DIR"
    fi
    
    ASSETS_FILE="$ASSETS_DIR/apk_hmac_signature.txt"
    cp "$OUTPUT_FILE" "$ASSETS_FILE"
    print_success "HMAC signature copied to assets: $ASSETS_FILE"
fi

# Display signature information
print_info "Signature Information:"
echo "  File: $OUTPUT_FILE"
echo "  Size: $(du -h "$OUTPUT_FILE" | cut -f1)"
echo "  Content: $(head -c 32 "$OUTPUT_FILE")..."

# Verify signature
print_info "Verifying signature..."
SIGNATURE_CONTENT=$(cat "$OUTPUT_FILE")
if [[ ${#SIGNATURE_CONTENT} -eq 64 ]]; then
    print_success "Signature verification passed (64 characters)"
else
    print_warning "Signature length is ${#SIGNATURE_CONTENT} characters (expected 64)"
fi

print_success "APK HMAC signing completed successfully!"
print_info "Next steps:"
echo "  1. Embed the signature in your app"
echo "  2. Use ApkHmacProtector.verifyApkIntegrity() at runtime"
echo "  3. Check for repackaging with ApkHmacProtector.detectRepackaging()"

# Clean up temporary files if needed
if [[ "$OUTPUT_FILE" != "$ASSETS_FILE" ]] && [[ -n "$ASSETS_DIR" ]]; then
    rm -f "$OUTPUT_FILE"
    print_info "Temporary signature file removed"
fi
