#!/usr/bin/env bash
# Official Cafe Bazaar bundle-signer: sign the exact published AAB, without exporting the key.
set -euo pipefail
umask 077

test "$#" -eq 2 || { echo 'Usage: generate_bazaar_bin.sh INPUT.aab OUTPUT.bin' >&2; exit 2; }
input=$(realpath "$1")
output=$(realpath -m "$2")
test -s "$input"
test ! -e "$output" || { echo 'Refusing to overwrite an existing BIN.' >&2; exit 1; }
: "${KEYSTORE_PATH:?}" "${STORE_PASSWORD:?}" "${KEY_PASSWORD:?}" "${KEY_ALIAS:?}"
expected_certificate=a335e71031ec78a7961656b2c36d8c53abcb3bdc28e265998cf6053fb94eb950
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
input_hash=$(sha256sum "$input" | cut -d' ' -f1)

# Check the public certificate in both the AAB and the existing keystore before signing.
keytool -printcert -jarfile "$input" > "$work/aab-certificate.txt"
tr -d ':' < "$work/aab-certificate.txt" | tr '[:upper:]' '[:lower:]' | grep -F "$expected_certificate" >/dev/null
keytool -exportcert -rfc -keystore "$KEYSTORE_PATH" -alias "$KEY_ALIAS" \
  -storepass:env STORE_PASSWORD > "$work/certificate.pem"
fingerprint=$(openssl x509 -in "$work/certificate.pem" -noout -fingerprint -sha256 | cut -d= -f2 | tr -d ':' | tr '[:upper:]' '[:lower:]')
test "$fingerprint" = "$expected_certificate"
jarsigner -verify "$input" > "$work/aab-verification.txt"
grep -F 'jar verified.' "$work/aab-verification.txt" >/dev/null

# Fixed official release; never use a third-party signer or generate a replacement key.
curl --fail --location --retry 3 --connect-timeout 20 --max-time 180 \
  https://github.com/cafebazaar/bundle-signer/releases/download/v0.1.13/bundlesigner-0.1.13.jar \
  --output "$work/bundlesigner.jar"
echo "Official bundle-signer 0.1.13 SHA256: $(sha256sum "$work/bundlesigner.jar" | cut -d' ' -f1)"

# The official tool derives the output name from text before the FIRST dot.
# Use app.aab in an isolated directory, then rename only the resulting BIN.
cp "$input" "$work/app.aab"
mkdir "$work/bin" "$work/apks"
java -Xmx2g -jar "$work/bundlesigner.jar" genbin \
  --bundle "$work/app.aab" --bin "$work/bin" \
  --v2-signing-enabled true --v3-signing-enabled false \
  --ks "$KEYSTORE_PATH" --ks-key-alias "$KEY_ALIAS" \
  --ks-pass env:STORE_PASSWORD --key-pass env:KEY_PASSWORD
test -s "$work/bin/app.bin"
head -n 2 "$work/bin/app.bin" | grep -Fx 'v2:true,v3:false' >/dev/null

# Reconstruct APKs from this AAB/BIN pair using only public signatures.
# Verify every split and universal APK; this catches a mismatched or damaged BIN.
java -Xmx2g -jar "$work/bundlesigner.jar" signbundle \
  --bundle "$work/app.aab" --bin "$work/bin/app.bin" --out "$work/apks"
sdk_root=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
test -n "$sdk_root"
build_tools=$(find "$sdk_root/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)
test -x "$build_tools/apksigner"
count=0
while IFS= read -r -d '' apk; do
  "$build_tools/apksigner" verify --verbose --print-certs "$apk" > "$work/apk-verification.txt"
  grep -Fi "$expected_certificate" "$work/apk-verification.txt" >/dev/null
  count=$((count + 1))
done < <(find "$work/apks" -type f -name '*.apk' -print0)
test "$count" -gt 1
test -s "$work/apks/universal.apk"
test "$(sha256sum "$input" | cut -d' ' -f1)" = "$input_hash"
mkdir -p "$(dirname "$output")"
cp "$work/bin/app.bin" "$output"
echo "Verified Cafe Bazaar AAB/BIN pair: $count signed APKs; original AAB SHA256 $input_hash"
