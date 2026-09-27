#!/usr/bin/env bash
# One-time setup of a fresh Ubuntu server (for example Oracle Cloud Always Free) for FitTracker.
#
#   git clone https://github.com/premisess/tracker.git
#   bash tracker/deploy/setup-server.sh
#
# Safe to run again: it skips whatever is already done, and never overwrites an existing deploy/.env.
set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$(dirname "$DEPLOY_DIR")"
HOME_DIR="$(dirname "$BACKEND_DIR")"
ENV_FILE="$DEPLOY_DIR/.env"

step() { printf '\n==> %s\n' "$1"; }

# Replaces KEY=... in the .env file. Values come through the environment, so any characters are safe.
set_env() {
    KEY="$1" VALUE="$2" awk 'BEGIN { k = ENVIRON["KEY"]; v = ENVIRON["VALUE"] }
        index($0, k "=") == 1 { print k "=" v; next } { print }' "$ENV_FILE" > "$ENV_FILE.tmp"
    mv "$ENV_FILE.tmp" "$ENV_FILE"
}

step "Docker"
if command -v docker >/dev/null 2>&1; then
    echo "Already installed."
else
    curl -fsSL https://get.docker.com | sudo sh
    sudo usermod -aG docker "$USER"
fi

step "Firewall on this machine (Oracle's Ubuntu images only allow SSH)"
if sudo iptables -S INPUT 2>/dev/null | grep -q 'REJECT'; then
    for port in 80 443; do
        if sudo iptables -C INPUT -p tcp --dport "$port" -j ACCEPT 2>/dev/null; then
            echo "Port $port already open."
        else
            # Insert just above the catch-all REJECT rule.
            line="$(sudo iptables -L INPUT --line-numbers | awk '/REJECT/ { print $1; exit }')"
            sudo iptables -I INPUT "$line" -p tcp --dport "$port" -j ACCEPT
            echo "Opened port $port."
        fi
    done
    if command -v netfilter-persistent >/dev/null 2>&1; then
        sudo netfilter-persistent save >/dev/null
    else
        echo "Note: netfilter-persistent isn't installed, so these rules last until the next reboot."
    fi
else
    echo "No blocking rules found."
fi

step "Frontend code"
if [ -d "$HOME_DIR/fitness-tracker-ui/.git" ]; then
    git -C "$HOME_DIR/fitness-tracker-ui" pull --ff-only
else
    git clone https://github.com/premisess/fitness-tracker-ui.git "$HOME_DIR/fitness-tracker-ui"
fi

step "Settings (deploy/.env)"
if [ -f "$ENV_FILE" ]; then
    echo "Keeping the existing deploy/.env. Edit it with: nano $ENV_FILE"
else
    cp "$DEPLOY_DIR/.env.example" "$ENV_FILE"
    chmod 600 "$ENV_FILE"

    read -r -p "DuckDNS name (just the name, e.g. fittracker): " duck_name
    duck_name="${duck_name%.duckdns.org}"
    read -r -s -p "DuckDNS token (hidden while typing): " duck_token; echo
    read -r -s -p "Gmail app password for fittrackers2026@gmail.com (hidden, spaces are fine): " mail_password; echo

    set_env SITE_ADDRESS "$duck_name.duckdns.org"
    set_env DUCKDNS_DOMAIN "$duck_name"
    set_env DUCKDNS_TOKEN "$duck_token"
    set_env MAIL_PASSWORD "${mail_password// /}"
    set_env DB_PASSWORD "$(openssl rand -hex 24)"
    set_env DB_ROOT_PASSWORD "$(openssl rand -hex 24)"
    echo "Saved. Database passwords were generated for you."
fi

step "Starting FitTracker (the first build takes several minutes)"
cd "$BACKEND_DIR"
sudo docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build

site="$(awk -F= '$1 == "SITE_ADDRESS" { print $2 }' "$ENV_FILE")"
step "Done"
echo "Open https://$site in a minute or two, once the HTTPS certificate is issued."
echo "Register your own account first: the first account becomes the admin."
echo
echo "Useful commands (run from $BACKEND_DIR):"
echo "  sudo docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps"
echo "  sudo docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs -f backend web duckdns"
