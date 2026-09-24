#!/usr/bin/env bash
#
# Student OS backend — one-shot secure VPS setup.
#
# Run this ON YOUR VPS (Ubuntu 22.04/24.04 recommended), as root or with
# sudo, from inside this same folder (the one containing this script,
# docker-compose.yml, src/, nginx/):
#
#   sudo bash setup.sh
#
# It is safe to re-run: it will skip anything already installed/configured.
#
set -euo pipefail

# ── 0. Sanity checks ────────────────────────────────────────────────────
if [[ $EUID -ne 0 ]]; then
  echo "❌ Please run this script as root (or with sudo)." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [[ ! -f "docker-compose.yml" ]]; then
  echo "❌ docker-compose.yml not found next to this script. Are you in the right folder?" >&2
  exit 1
fi

echo "════════════════════════════════════════════════════"
echo "  Student OS Backend — Secure VPS Setup"
echo "════════════════════════════════════════════════════"
echo

# ── 1. Gather info from you ──────────────────────────────────────────────
if [[ -f .env ]]; then
  echo "ℹ️  Found an existing .env — reusing it. Delete .env first if you want to start fresh."
  # shellcheck disable=SC1091
  source .env
else
  read -rp "🌐 Domain name that points to this server's IP (e.g. api.myapp.ir): " DOMAIN
  read -rp "📧 Email address for HTTPS certificate renewal notices: " CERTBOT_EMAIL

  DB_USER="studentos"
  DB_NAME="studentos"
  DB_PASSWORD="$(openssl rand -base64 24 | tr -d '/+=' | cut -c1-32)"
  JWT_ACCESS_SECRET="$(openssl rand -hex 48)"

  cat > .env <<EOF
DOMAIN=${DOMAIN}
CERTBOT_EMAIL=${CERTBOT_EMAIL}
DB_USER=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
DB_NAME=${DB_NAME}
JWT_ACCESS_SECRET=${JWT_ACCESS_SECRET}
EOF
  chmod 600 .env
  echo "✅ Generated .env with a random DB password and JWT secret (never shared, never leaves this server)."
fi

# shellcheck disable=SC1091
source .env

echo
echo "→ Domain: $DOMAIN"
echo "→ Certbot email: $CERTBOT_EMAIL"
echo

# ── 2. Verify DNS before requesting a certificate ────────────────────────
SERVER_IP="$(curl -fsSL https://ifconfig.me || curl -fsSL https://api.ipify.org || true)"
RESOLVED_IP="$(getent hosts "$DOMAIN" | awk '{print $1}' | head -n1 || true)"
if [[ -n "$SERVER_IP" && -n "$RESOLVED_IP" && "$SERVER_IP" != "$RESOLVED_IP" ]]; then
  echo "⚠️  Warning: $DOMAIN currently resolves to $RESOLVED_IP, but this server's IP looks like $SERVER_IP."
  echo "   Fix your DNS A record first, or the HTTPS certificate step below will fail."
  read -rp "   Continue anyway? [y/N] " CONTINUE_ANYWAY
  [[ "$CONTINUE_ANYWAY" =~ ^[Yy]$ ]] || exit 1
fi

# ── 3. Install Docker + Compose plugin ───────────────────────────────────
if ! command -v docker &>/dev/null; then
  echo "📦 Installing Docker..."
  curl -fsSL https://get.docker.com | sh
  systemctl enable --now docker
else
  echo "✅ Docker already installed."
fi

# ── 4. Install Nginx + Certbot ────────────────────────────────────────────
if ! command -v nginx &>/dev/null; then
  echo "📦 Installing Nginx..."
  apt-get update -y
  apt-get install -y nginx
else
  echo "✅ Nginx already installed."
fi

if ! command -v certbot &>/dev/null; then
  echo "📦 Installing Certbot..."
  apt-get update -y
  apt-get install -y certbot python3-certbot-nginx
else
  echo "✅ Certbot already installed."
fi

# ── 5. Firewall ───────────────────────────────────────────────────────────
if command -v ufw &>/dev/null; then
  ufw allow 22/tcp  >/dev/null 2>&1 || true
  ufw allow 80/tcp  >/dev/null 2>&1 || true
  ufw allow 443/tcp >/dev/null 2>&1 || true
  echo "✅ Firewall rules ensured (22, 80, 443 open)."
fi

# ── 6. Temporary HTTP-only Nginx config (needed for the ACME challenge) ──
mkdir -p /var/www/certbot
cat > /etc/nginx/sites-available/studentos <<EOF
server {
    listen 80;
    server_name ${DOMAIN};
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    location / {
        return 200 'Student OS backend — provisioning HTTPS...';
        add_header Content-Type text/plain;
    }
}
EOF
ln -sf /etc/nginx/sites-available/studentos /etc/nginx/sites-enabled/studentos
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl reload nginx

# ── 7. Obtain the HTTPS certificate ──────────────────────────────────────
if [[ ! -d "/etc/letsencrypt/live/${DOMAIN}" ]]; then
  echo "🔐 Requesting HTTPS certificate for ${DOMAIN}..."
  certbot certonly --webroot -w /var/www/certbot \
    -d "$DOMAIN" \
    --non-interactive --agree-tos -m "$CERTBOT_EMAIL"
else
  echo "✅ Certificate for $DOMAIN already exists."
fi

# ── 8. Install the final HTTPS Nginx config ──────────────────────────────
sed "s/__DOMAIN__/${DOMAIN}/g" nginx/studentos.conf.template > /etc/nginx/sites-available/studentos
nginx -t && systemctl reload nginx
echo "✅ Nginx is now serving HTTPS on ${DOMAIN}."

# Auto-renewal is installed by certbot's own systemd timer by default — just
# confirm it's enabled.
systemctl enable --now certbot.timer 2>/dev/null || true

# ── 9. Build and start the backend ───────────────────────────────────────
echo "🚀 Building and starting the backend (this can take a minute the first time)..."
if docker compose version &>/dev/null; then
  docker compose up -d --build
else
  docker-compose up -d --build
fi

# ── 10. Health check ──────────────────────────────────────────────────────
echo -n "⏳ Waiting for the service to become healthy"
for i in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1:3000/health" &>/dev/null; then
    echo
    echo "✅ Backend is up and healthy."
    break
  fi
  echo -n "."
  sleep 2
  if [[ "$i" -eq 30 ]]; then
    echo
    echo "❌ Backend did not become healthy in time. Check logs with:"
    echo "     docker compose logs -f app"
    exit 1
  fi
done

echo
echo "════════════════════════════════════════════════════"
echo "  ✅ All done!"
echo "════════════════════════════════════════════════════"
echo "  API base URL for the Android app:"
echo "    https://${DOMAIN}/api"
echo
echo "  Try it:"
echo "    curl https://${DOMAIN}/health"
echo
echo "  Useful commands:"
echo "    docker compose logs -f app     # view live logs"
echo "    docker compose restart app     # restart just the app"
echo "    docker compose down            # stop everything"
echo "    sudo bash setup.sh             # safe to re-run any time"
echo
echo "  Your secrets live in: ${SCRIPT_DIR}/.env — back this file up somewhere"
echo "  safe (e.g. a password manager). Do NOT commit it to git."
echo "════════════════════════════════════════════════════"
