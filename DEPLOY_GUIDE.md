# QUOTEME AWS 배포 가이드

## 전체 순서

1. RDS (MySQL) 생성
2. EC2 생성
3. 보안 그룹 설정
4. EC2 초기 세팅
5. RDS DB/유저 초기화
6. Nginx + SSL 설정
7. .env 작성 후 첫 배포

---

## 1. RDS (MySQL) 생성

AWS Console → RDS → 데이터베이스 생성

| 항목 | 값 |
|------|----|
| 엔진 | MySQL 8.0 |
| 템플릿 | **프리 티어** (개발/소규모) 또는 프로덕션 |
| DB 인스턴스 클래스 | db.t3.micro (프리 티어) |
| 스토리지 | gp2, 20GB |
| 마스터 사용자 이름 | admin |
| 마스터 암호 | (안전한 암호 설정) |
| 퍼블릭 액세스 | **아니요** (EC2에서만 접근) |
| VPC | EC2와 **동일한 VPC** 선택 |
| 초기 데이터베이스 이름 | 비워 두기 (SQL로 직접 생성) |

> 생성 완료 후 **엔드포인트** 복사 → `.env`의 `DB_HOST`에 입력

---

## 2. EC2 생성

AWS Console → EC2 → 인스턴스 시작

| 항목 | 값 |
|------|----|
| AMI | Ubuntu Server 22.04 LTS |
| 인스턴스 유형 | t3.small (최소) / t3.medium (권장) |
| 키 페어 | 새로 생성 후 `.pem` 파일 보관 |
| 스토리지 | gp3, 20GB |

---

## 3. 보안 그룹 설정

### EC2 보안 그룹 (인바운드 규칙)

| 포트 | 프로토콜 | 소스 | 용도 |
|------|----------|------|------|
| 22 | TCP | 내 IP | SSH |
| 80 | TCP | 0.0.0.0/0 | HTTP (→ HTTPS 리다이렉트) |
| 443 | TCP | 0.0.0.0/0 | HTTPS |

> **8070 포트는 열지 않습니다.** Nginx가 내부에서 프록시합니다.

### RDS 보안 그룹 (인바운드 규칙)

| 포트 | 프로토콜 | 소스 | 용도 |
|------|----------|------|------|
| 3306 | TCP | EC2 보안 그룹 ID | EC2에서만 접근 허용 |

> EC2의 보안 그룹 ID를 소스로 지정하면 IP 변경 없이 안전하게 연결됩니다.

---

## 4. EC2 초기 세팅

```bash
# 로컬에서 EC2에 스크립트 업로드
scp -i your-key.pem -r scripts/ ubuntu@<EC2_IP>:~/

# EC2 SSH 접속
ssh -i your-key.pem ubuntu@<EC2_IP>

# 초기화 스크립트 실행
bash ~/scripts/ec2-init.sh

# docker 그룹 적용을 위해 재접속
exit
ssh -i your-key.pem ubuntu@<EC2_IP>

# Docker 동작 확인
docker ps
```

---

## 5. RDS DB / 유저 초기화

```bash
# EC2에서 실행 (mysql-client 설치)
sudo apt-get install -y mysql-client

# RDS 접속 (admin 계정)
mysql -h <RDS_ENDPOINT> -u admin -p

# SQL 파일 실행
# rds-init.sql 의 'strong_password_here' 를 실제 비밀번호로 바꾼 뒤:
mysql -h <RDS_ENDPOINT> -u admin -p < ~/scripts/rds-init.sql
```

---

## 6. Nginx + SSL 설정

```bash
# nginx 설정 파일 업로드 (로컬에서)
scp -i your-key.pem -r nginx/ ubuntu@<EC2_IP>:~/

# EC2에서 실행
bash ~/nginx/setup-nginx-ssl.sh your@email.com
```

---

## 7. .env 작성 후 첫 배포

```bash
# EC2에서 .env 수정
nano /srv/quoteme/deploy/.env
# → .env.example 참고해서 모든 값 입력

# GitHub Secrets 등록 확인 (GitHub 저장소 → Settings → Secrets)
# 필수: EC2_HOST, EC2_USER, EC2_SSH_KEY, DOCKERHUB_USERNAME, DOCKERHUB_TOKEN
```

GitHub `main` 브랜치에 push → 자동 배포 시작

---

## 배포 후 확인

```bash
# EC2에서 컨테이너 상태 확인
cd /srv/quoteme/deploy
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f app

# 외부에서 헬스체크
curl https://api.quoteme.shop/
```

---

## GitHub Secrets 목록

| Secret 이름 | 값 |
|-------------|-----|
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | `.pem` 파일 내용 전체 (-----BEGIN ~ -----END) |
| `DOCKERHUB_USERNAME` | Docker Hub 사용자명 |
| `DOCKERHUB_TOKEN` | Docker Hub Access Token |
