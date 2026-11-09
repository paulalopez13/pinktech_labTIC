#!/bin/sh
set -eu

if [ -n "${SMTP_CA_CERT_BASE64:-}" ]; then
  cert_file="/tmp/smtp-local-root-ca.der"
  printf '%s' "$SMTP_CA_CERT_BASE64" | base64 -d > "$cert_file"
  "$JAVA_HOME/bin/keytool" -delete -alias smtp-local-root-ca -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit >/dev/null 2>&1 || true
  "$JAVA_HOME/bin/keytool" -importcert -noprompt -trustcacerts -alias smtp-local-root-ca \
    -file "$cert_file" -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit >/dev/null
  rm -f "$cert_file"
fi

exec java -jar /app/app.jar
