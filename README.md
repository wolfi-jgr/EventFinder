# EventFinder (Skeleton)

This is a starter skeleton for a fullstack learning project.

## Stack
- Backend: Spring Boot + Hibernate (Spring Data JPA)
- DB: Postgres
- Frontend: React (Vite)
- Dev orchestration: Docker Compose

## Quick start (Docker)
1) From this folder, run:
   docker compose up --build
2) Frontend: http://localhost:5173
3) Backend: http://localhost:8080/api/health

## Local dev (without Docker)
Backend:
- Configure Postgres and set env vars shown in docker-compose.yml
- Run: mvn spring-boot:run

Frontend:
- npm install
- npm run dev

## Notes
- The scraper is a stub (see ScrapeService). Replace it later with real HTTP scraping/API calls.
- Data caching/storage is done in Postgres for performance.
