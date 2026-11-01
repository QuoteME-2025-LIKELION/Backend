#!/bin/bash
# EC2에서 1회 실행하는 Nginx + Let's Encrypt 설정 스크립트
# 사용법: bash setup-nginx-ssl.sh your@email.com
# 전제조건: api.quoteme.shop 의 DNS A 레코드가 이 EC2 IP를 가리켜야 합니다.

set -e

EMAIL=${1:?"이메일 주소를 인수로 전달하세요. 예) bash setup-nginx-ssl.sh your@email.com"}
DOMAIN="api.quoteme.shop"
CONF_SRC="$(dirname "$0")/api.quoteme.shop.conf"

echo "=== [1/4] Nginx + Certbot 설치 ==="
sudo apt-get update -y
sudo apt-get install -y nginx certbot python3-certbot-nginx

echo "=== [2/4] Nginx 설정 파일 복사 ==="
sudo mkdir -p /var/www/certbot

# 인증서가 아직 없으므로 먼저 HTTP 전용 설정으로 Nginx를 기동합니다.
sudo tee /etc/nginx/sites-available/${DOMAIN}.conf > /dev/null <<EOF
server {
  listen 80;
  server_name ${DOMAIN};

  location /.well-known/acme-challenge/ {
    root /var/www/certbot;
  }

  location / {
    return 404;
  }
}
EOF
sudo ln -sf /etc/nginx/sites-available/${DOMAIN}.conf /etc/nginx/sites-enabled/${DOMAIN}.conf

# default 사이트 비활성화 (충돌 방지)
sudo rm -f /etc/nginx/sites-enabled/default

sudo nginx -t
sudo systemctl restart nginx

echo "=== [3/4] Let's Encrypt 인증서 발급 ==="
sudo certbot certonly --webroot \
  --webroot-path /var/www/certbot \
  -d "$DOMAIN" \
  --non-interactive \
  --agree-tos \
  --email "$EMAIL" \
  --keep-until-expiring

# 인증서 발급 후 443 SSL + 80 리다이렉트 설정으로 교체합니다.
sudo cp "$CONF_SRC" /etc/nginx/sites-available/${DOMAIN}.conf
sudo nginx -t
sudo systemctl reload nginx

echo "=== [4/4] 자동 갱신 확인 ==="
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer
sudo certbot renew --dry-run

echo ""
echo "완료! https://${DOMAIN} 으로 접근 가능합니다."
