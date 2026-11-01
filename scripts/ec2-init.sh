#!/bin/bash
# EC2 (Ubuntu 22.04 LTS) 최초 1회 실행 스크립트
# 사용법: bash ec2-init.sh
# 수행 내용: Docker, Docker Compose 설치 + 배포 디렉토리 생성

set -e

echo "========================================"
echo " QUOTEME EC2 초기 환경 설정"
echo "========================================"

# ── 1. 시스템 업데이트 ────────────────────────────────────────
echo ""
echo "[1/5] 시스템 패키지 업데이트..."
sudo apt-get update -y
sudo apt-get upgrade -y

# ── 2. Docker 설치 ────────────────────────────────────────────
echo ""
echo "[2/5] Docker 설치..."
sudo apt-get install -y ca-certificates curl gnupg

sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update -y
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 현재 유저를 docker 그룹에 추가 (sudo 없이 docker 사용)
sudo usermod -aG docker "$USER"

sudo systemctl enable docker
sudo systemctl start docker

# ── 3. 배포 디렉토리 생성 ─────────────────────────────────────
echo ""
echo "[3/5] 배포 디렉토리 생성..."
sudo mkdir -p /srv/quoteme/deploy
sudo chown "$USER":"$USER" /srv/quoteme/deploy

# ── 4. .env 파일 생성 안내 ───────────────────────────────────
echo ""
echo "[4/5] .env 파일 템플릿 복사..."
if [ -f "$(dirname "$0")/../.env.example" ]; then
  cp "$(dirname "$0")/../.env.example" /srv/quoteme/deploy/.env
  echo "  → /srv/quoteme/deploy/.env 에 복사됐습니다. 실제 값으로 채워 주세요."
else
  touch /srv/quoteme/deploy/.env
  echo "  → /srv/quoteme/deploy/.env 파일을 직접 작성해 주세요."
fi

# ── 5. 방화벽 설정 ────────────────────────────────────────────
echo ""
echo "[5/5] UFW 방화벽 설정..."
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable

echo ""
echo "========================================"
echo " 완료!"
echo " 주의: docker 그룹 적용을 위해 로그아웃 후 재접속하세요."
echo "   다음 단계: nano /srv/quoteme/deploy/.env  (실제 값 입력)"
echo "========================================"
