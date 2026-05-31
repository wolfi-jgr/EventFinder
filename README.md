# EventFinder (Skeleton)

This is a starter skeleton for a fullstack learning project.

## Stack
- Backend: Spring Boot + Hibernate (Spring Data JPA)
- DB: Postgres
- Frontend: React (Vite)
- Dev orchestration: Docker Compose

## Quick start (Docker)
1) Create a `.env` file from the template:
   ```bash
   cp .env.example .env
   # Edit .env and set secure credentials
   ```
2) Run from this folder:
   ```bash
   docker compose up --build
   ```
3) Frontend: http://localhost:5173
4) Backend: http://localhost:8080/api/health

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
- Refer to the mobile-expo directory for setup instructions.

## Shared frontend config (Vite + Expo)
- Shared UI/app/API defaults now live in `shared/frontendConfig.js`.
- Vite web app consumes this via alias `@shared/frontendConfig`.
- Expo app consumes this via `../shared/frontendConfig` (enabled by `mobile-expo/metro.config.js`).
- Update colors/app defaults once in `shared/frontendConfig.js` to keep both clients aligned.


## Transition to PWA deployment
- in progress

## Security Notes
- **Credentials**: Database and Spring Boot passwords are managed via environment variables in `.env` (not tracked in git).
- **CORS**: Update `APP_CORS_ORIGINS` in `.env` for production deployments.
- Before deploying to production, ensure all environment variables are securely configured and not hardcoded in source files.
- See `.env.example` for the list of required environment variables.