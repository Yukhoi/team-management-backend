# Production deployment quick guide

Prerequisites
- VPS (Ubuntu 24.04 recommended)
- Docker & Docker Compose installed
- Domain name pointing to the VPS

1) Prepare VPS
   - Create a non-root user, open ports 80 and 443 (and optionally 22 for SSH) on the firewall.

2) Install Docker
   - https://docs.docker.com/engine/install/ubuntu/

3) Install Docker Compose (if not included)
   - Use the distro package or docker's official instructions. On modern systems use `docker compose` plugin.

4) Upload project to VPS (git preferred)
   git clone <repo>
   cd team-management-backend

5) Create production environment file
   cp .env.prod.example .env.prod
   # fill in real secrets, do NOT commit .env.prod

6) Start services
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build

7) Check status & logs
   docker compose -f docker-compose.prod.yml ps
   docker compose -f docker-compose.prod.yml logs -f gateway-service

8) Update / deploy new version
   git pull
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build

Notes and production recommendations
- Use a secrets manager instead of environment files when possible.
- Rotate JWT secret and DB passwords regularly.
- For high availability consider external managed Postgres / Kafka and multiple gateway instances behind a load balancer.
- Monitoring and alerts: integrate Prometheus / Grafana on actuator endpoints.

