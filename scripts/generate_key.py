#!/usr/bin/env python3
"""
CatchUP POS License Key Generator

Usage:
    python generate_key.py

Generates a license key in the format: CATCHUP-XXXX-XXXX-XXXX
"""

import hmac
import hashlib
import random
import string
import sys

# This MUST match the HMAC_SECRET in LicenseManager.kt
HMAC_SECRET = "C4tchUP-P0S-S3cr3t-K3y-2024!@#$"

CHARS = string.ascii_uppercase + string.digits  # A-Z, 0-9


def compute_hmac(data: str) -> str:
    """Compute HMAC-SHA256 and return first 4 hex chars (uppercase)."""
    mac = hmac.new(HMAC_SECRET.encode(), data.encode(), hashlib.sha256)
    return mac.hexdigest()[:4].upper()


def generate_key() -> str:
    """Generate a license key: CATCHUP-XXXX-XXXX-XXXX"""
    # Generate 8 random alphanumeric characters for payload
    payload = ''.join(random.choices(CHARS, k=8))
    first_half = payload[:4]
    second_half = payload[4:]

    # Compute HMAC of the payload
    hmac_value = compute_hmac(payload)

    return f"CATCHUP-{first_half}-{second_half}-{hmac_value}"


def main():
    count = 1
    if len(sys.argv) > 1:
        try:
            count = int(sys.argv[1])
        except ValueError:
            print("Usage: python generate_key.py [count]")
            sys.exit(1)

    print(f"\n{'='*50}")
    print(f"  CatchUP POS License Key Generator")
    print(f"{'='*50}\n")

    for i in range(count):
        key = generate_key()
        if count == 1:
            print(f"  License Key: {key}\n")
        else:
            print(f"  {i+1}. {key}")

    print(f"{'='*50}")
    print(f"  Total keys generated: {count}")
    print(f"{'='*50}\n")


if __name__ == "__main__":
    main()
