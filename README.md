# Java CI/CD AWS Spring Boot Project

This repository demonstrates a production-minded Spring Boot REST API built with Java 21, packaged as a Docker image, deployed on AWS EC2 with Nginx and automated GitHub Actions CI/CD.

## What this project includes

### Phase 1: Spring Boot foundation
- Java 21 + Spring Boot 4.0.3
- Endpoints:
  - `GET /hello` with optional `name` query param
  - `GET /health` (custom app health response)
  - `GET /info` (app/version metadata)
- Clean response format via `ApiResponse<T>`
- Actuator enabled with `health`, `info`, `metrics`, `prometheus` endpoints
- Logging added at endpoint level (no noisy global request logs)

### Phase 2: Monitoring
- Spring Boot Actuator configured for production checks and visibility
- Build metadata exposed on `/actuator/info` and `/actuator/health`
- Optional Prometheus endpoint when scraper dependency is included

### Phase 3: First deployment on EC2
- Ubuntu 24.04 EC2 (`t3.micro`) for free-tier-friendly compute
- Java 21 installed
- App run using `systemd` service (`spring-app.service`) for restart on failure

### Phase 4: Nginx reverse proxy
- Nginx on host listens on `80`
- Proxies requests to Spring Boot on `127.0.0.1:8080`
- Allows cleaner public traffic handling and future HTTPS migration

### Phase 5: Dockerization
- Multi-stage `Dockerfile` to produce small, production-oriented image
- Build args via env vars:
  - `SERVER_PORT` (default `8080`)
  - `SPRING_PROFILES_ACTIVE`
  - `JAVA_OPTS`
- Container can be started with restart policy (`--restart unless-stopped`)

### Phase 6: CI/CD
- GitHub Actions workflow builds JAR + Docker image and pushes to Docker Hub
- Deploys to EC2 over SSH, pulls new image, replaces old container
- Adds image rollback strategy using previous running image on failed health check

---

## Repo structure

```text
.
├── .github/
│   └── workflows/
│       └── ci-cd.yml
├── src/
│   └── main/
│       ├── java/com/sanju/app/...
│       └── resources/application.yml
├── Dockerfile
├── .dockerignore
├── pom.xml
└── README.md
```

---

## Quick local setup

### Prerequisites
- Java 21
- Maven wrapper (`./mvnw`) or Maven installed
- Docker (for container path)

### Run locally (without Docker)

```powershell
cd C:\Users\Welcome\Desktop\ust-project\ust-project
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

### Validate local endpoints

```powershell
curl.exe -i "http://localhost:8080/hello"
curl.exe -i "http://localhost:8080/hello?name=Sanju"
curl.exe -i "http://localhost:8080/health"
curl.exe -i "http://localhost:8080/info"
curl.exe -i "http://localhost:8080/actuator/health"
```

---

## Docker usage

### Build image

```powershell
docker build -t sanjumarri7/ust-spring-api:latest .
```

### Run container locally

```powershell
docker run -d --name spring-app `
  --restart unless-stopped `
  -p 8080:8080 `
  -e SERVER_PORT=8080 `
  -e JAVA_OPTS="-Xms256m -Xmx512m" `
  -e SPRING_PROFILES_ACTIVE=dev `
  sanjumarri7/ust-spring-api:latest
```

### Validate

```powershell
curl.exe -i http://localhost:8080/health
curl.exe -i "http://localhost:8080/hello?name=Sanju"
```

### Logs / safe restart

```powershell
docker logs -f spring-app
docker restart spring-app
```

### Remove stale container and rerun

```powershell
docker stop spring-app
docker rm spring-app
```

---

## AWS EC2 deployment (container)

### One-time EC2 prep
- Install Docker (`sudo apt install -y docker.io`)
- Add user: `sudo usermod -aG docker ubuntu` (re-login required)

### Deploy current image

```bash
sudo docker pull sanjumarri7/ust-spring-api:latest
sudo docker stop spring-app || true
sudo docker rm spring-app || true
sudo docker run -d --name spring-app \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SERVER_PORT=8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  sanjumarri7/ust-spring-api:latest
```

### Verify on EC2

```bash
curl -i http://localhost:8080/health
```

### Nginx (optional, recommended)
- Nginx should listen on port 80
- Reverse proxy `proxy_pass http://127.0.0.1:8080`
- Security group:
  - open `22` (SSH) from your IP
  - open `80` from `0.0.0.0/0`
  - close/remove direct `8080` from public internet once Nginx is stable

---

## GitHub Actions CI/CD

Workflow file: `.github/workflows/ci-cd.yml`

### Required GitHub Secrets

| Secret | Value |
| --- | --- |
| `DOCKERHUB_USERNAME` | `sanjumarri7` |
| `DOCKERHUB_TOKEN` | Docker Hub token (PAT) |
| `EC2_HOST` | EC2 public IP or DNS |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | PEM private key content (full block text) |
| `EC2_SSH_PORT` | `22` (optional) |
| `SPRING_PROFILE` | `prod` (optional) |

### Workflow behavior
1. Trigger on push to `main`
2. Build JAR with Maven
3. Build Docker image
4. Push image to Docker Hub with tags:
   - `${{ DOCKERHUB_USERNAME }}/ust-spring-api:<github_sha>`
   - `${{ DOCKERHUB_USERNAME }}/ust-spring-api:latest`
5. SSH into EC2
6. Pull latest image
7. Replace running container
8. Health-check `/health`
9. Rollback to previous image on failure

---

## Rollback strategy

- Keep previous container image reference before replacement.
- Deploy script attempts automatic rollback if new image fails health check.
- To force manual rollback, re-run CI with a known prior tag or run deploy script with that tag.

---

## Interview talking points

- Why Docker multi-stage build?
  - small image, repeatable artifact, clean production image.
- Why systemd vs Docker for this project?
  - started with systemd for bare-metal deployment, moved to container strategy for immutability and repeatability.
- Why reverse proxy (Nginx)?
  - separates internet-facing concerns from application, supports TLS later, simplifies rolling strategies.
- Why SG tightening?
  - security hardening: only expose what is necessary (`80` + `22`) and protect app port from internet.
- How monitoring works?
  - custom health endpoint + Actuator + optional Prometheus metrics.

---

## Current known endpoints

| Method | Path | Description |
| --- | --- | --- |
| GET | `/hello` | Returns `{status, data.message, timestamp}` |
| GET | `/health` | Custom health payload |
| GET | `/info` | App name/version/build time payload |
| GET | `/actuator/health` | Actuator health |
| GET | `/actuator/info` | Actuator info |
| GET | `/actuator/metrics` | Actuator metrics |
| GET | `/actuator/prometheus` | Prometheus scrape |

---

## Notes
- The app was originally tested on both bare JAR (systemd) and Docker container deployments.
- Current standard path is Docker on EC2 with GitHub Actions-driven deploy.
- Public demo endpoints in this repo are intentionally simple to reflect interview-readiness and low complexity.
