#!/usr/bin/env bash
set -euo pipefail

CERT_DIR="$(cd "$(dirname "$0")/.." && pwd)/certs"
mkdir -p "$CERT_DIR"

pushd "$CERT_DIR" >/dev/null

rm -f *.key *.pem *.csr *.srl *.p12 *.ext

openssl genrsa -out ca.key 4096
openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 \
  -out ca.pem -subj "/C=US/ST=CA/L=SF/O=SDTP/OU=Dev/CN=SDTP-CA"

gen_server_cert() {
  local name="$1"
  local cn="$2"
  local san="$3"

  openssl genrsa -out "${name}.key" 2048
  openssl req -new -key "${name}.key" -out "${name}.csr" \
    -subj "/C=US/ST=CA/L=SF/O=SDTP/OU=Validator/CN=${cn}"

  cat > "${name}.ext" <<EOT
subjectAltName=${san}
extendedKeyUsage=serverAuth
EOT

  openssl x509 -req -in "${name}.csr" -CA ca.pem -CAkey ca.key -CAcreateserial \
    -out "${name}.pem" -days 825 -sha256 -extfile "${name}.ext"
}

gen_server_cert "validator1" "validator1" "DNS:validator1,DNS:localhost,IP:127.0.0.1"
gen_server_cert "validator2" "validator2" "DNS:validator2,DNS:localhost,IP:127.0.0.1"

openssl genrsa -out coordinator.key 2048
openssl req -new -key coordinator.key -out coordinator.csr \
  -subj "/C=US/ST=CA/L=SF/O=SDTP/OU=Coordinator/CN=localhost"
cat > coordinator.ext <<EOT
subjectAltName=DNS:localhost,IP:127.0.0.1
extendedKeyUsage=serverAuth
EOT
openssl x509 -req -in coordinator.csr -CA ca.pem -CAkey ca.key -CAcreateserial \
  -out coordinator.pem -days 825 -sha256 -extfile coordinator.ext
openssl pkcs12 -export -in coordinator.pem -inkey coordinator.key \
  -out coordinator-keystore.p12 -name coordinator -password pass:changeit

# outbound client cert for coordinator -> validators mTLS
openssl genrsa -out coordinator-client.key 2048
openssl req -new -key coordinator-client.key -out coordinator-client.csr \
  -subj "/C=US/ST=CA/L=SF/O=SDTP/OU=Coordinator/CN=coordinator-client"
cat > coordinator-client.ext <<EOT
extendedKeyUsage=clientAuth
EOT
openssl x509 -req -in coordinator-client.csr -CA ca.pem -CAkey ca.key -CAcreateserial \
  -out coordinator-client.pem -days 825 -sha256 -extfile coordinator-client.ext
openssl pkcs12 -export -in coordinator-client.pem -inkey coordinator-client.key \
  -out coordinator-client-keystore.p12 -name coordinator-client -password pass:changeit

rm -f *.csr *.srl *.ext
popd >/dev/null

echo "Certificates generated in $CERT_DIR"
