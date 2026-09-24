#!/usr/bin/env bash
#
# Computes the SHA-256 SPKI pin(s) needed for the Android app's
# network_security_config.xml. Run this from ANY machine with openssl
# (your laptop is fine — it doesn't need to run on the VPS), after
# setup.sh has successfully issued the HTTPS certificate.
#
# Usage:
#   bash get-ssl-pin.sh api.yourdomain.com
#
set -euo pipefail

DOMAIN="${1:-}"
if [[ -z "$DOMAIN" ]]; then
  echo "Usage: bash get-ssl-pin.sh <your-domain>" >&2
  exit 1
fi

echo "Fetching the certificate chain for $DOMAIN..."
CHAIN="$(mktemp)"
echo | openssl s_client -servername "$DOMAIN" -connect "$DOMAIN:443" -showcerts 2>/dev/null > "$CHAIN"

echo
echo "Leaf certificate pin (put this as the FIRST <pin> in network_security_config.xml):"
openssl x509 -in "$CHAIN" -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64

echo
echo "Note: to also get the intermediate certificate's pin (recommended as a"
echo "backup pin so the app survives Certbot's automatic leaf renewal), open"
echo "$CHAIN in a text editor, find the SECOND '-----BEGIN CERTIFICATE-----'"
echo "block, save just that block as intermediate.pem, then run:"
echo "  openssl x509 -in intermediate.pem -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64"

rm -f "$CHAIN"
