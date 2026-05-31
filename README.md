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

## Mobile (Expo)
- A separate mobile client is available in `mobile-expo/`.
- Setup guide: `EXPO_MOBILE.md`
- Includes: localtunnel setup, tunnel password notes, and troubleshooting for phone testing.

## Shared frontend config (Vite + Expo)
- Shared UI/app/API defaults now live in `shared/frontendConfig.js`.
- Vite web app consumes this via alias `@shared/frontendConfig`.
- Expo app consumes this via `../shared/frontendConfig` (enabled by `mobile-expo/metro.config.js`).
- Update colors/app defaults once in `shared/frontendConfig.js` to keep both clients aligned.


## Transition to PWA deployment
- in progress