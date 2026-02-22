# 🏠 Local Development Guide

This guide explains how to run EventFinder locally for development and testing.

---

## 🚀 Quick Start - Local Development

### Option 1: Docker Compose (Recommended)

**Start the app:**
```powershell
docker-compose up
```

**Access the app:**
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api
- Database: localhost:5432

**Docker volumes:**
- `db_data` - PostgreSQL database storage (persists between restarts)
- `scraper_data` - Cached HTML from scraped websites (persists between restarts)

**Stop the app:**
```powershell
# Press Ctrl+C, then:
docker-compose down
```

---

## 🧪 Testing Production Build Locally

Before deploying to cloud, test the production build:

```powershell
docker-compose -f docker-compose.prod.yml up --build
```

**Access the production build:**
- Frontend: http://localhost:3000 (note different port!)
- Backend: http://localhost:8080

This uses the same Dockerfiles that will be deployed to Railway/Render.

**Clean up:**
```powershell
docker-compose -f docker-compose.prod.yml down
```

---

## 📱 Testing on Mobile (Same WiFi)

To test the PWA on your phone while developing locally:

### Step 1: Find Your Computer's IP
```powershell
ipconfig
```
Look for your WiFi adapter's IPv4 Address (e.g., `192.168.1.105`)

### Step 2: Temporary Change for Mobile Testing

**Option A: Environment Variable (Easiest)**
```powershell
# Set environment variable before starting
$env:VITE_API_BASE="http://192.168.1.105:8080"
docker-compose up
```

**Option B: Edit docker-compose.yml**
```yaml
frontend:
  environment:
    VITE_API_BASE: http://192.168.1.105:8080  # Your actual IP
```

### Step 3: Access from Phone
Open Safari/Chrome on your phone: `http://192.168.1.105:5173`

**Don't forget to change it back to `localhost` after testing!**

---

## 🔧 Development Modes Explained

### Development Mode (docker-compose.yml)
- **Frontend**: Vite dev server with hot reload
- **Backend**: Maven with spring-boot:run (auto-restart on changes)
- **Fast**: Changes reflect immediately
- **Use for**: Daily development

### Production Mode (docker-compose.prod.yml)
- **Frontend**: Optimized production build
- **Backend**: Compiled JAR with JRE
- **Smaller**: Multi-stage Docker builds
- **Use for**: Testing before deployment

---

## 🗂️ Project Structure

```
eventfinder/
├── docker-compose.yml           # Development mode
├── docker-compose.prod.yml      # Production testing mode
├── backend/
│   ├── Dockerfile              # Production backend image
│   ├── src/                    # Java source code
│   └── pom.xml                 # Maven dependencies
├── frontend/
│   ├── Dockerfile              # Production frontend image
│   ├── src/
│   │   ├── config.js          # API configuration (handles dev/prod)
│   │   ├── App.jsx            # Main app component
│   │   └── ...
│   ├── public/
│   │   ├── manifest.json      # PWA manifest
│   │   └── service-worker.js  # PWA service worker
│   └── package.json
└── ...
```

---

## 🔄 How API Configuration Works

The app automatically detects the environment:

### In Development (npm run dev):
```javascript
// config.js automatically uses:
API_BASE = "http://localhost:8080"
```

### In Docker Compose:
```javascript
// config.js uses VITE_API_BASE from environment:
API_BASE = process.env.VITE_API_BASE  // e.g., http://localhost:8080
```

### In Production (Railway/Render):
```javascript
// config.js uses VITE_API_BASE set during build:
API_BASE = "https://your-backend.railway.app"
```

**You don't need to change anything!** The [config.js](frontend/src/config.js) handles it automatically.

---

## 🕷️ Web Scraping System

EventFinder uses a **rule-based generic scraper** to extract events from multiple websites.

### How It Works

1. **ScrapeRule Configuration** - Database stores CSS selectors/regex patterns for each website
2. **HTML Caching** - Downloads HTML once per day, stores in `/app/scraper-data` volume
3. **Generic Extraction** - Single scraper handles all sites using stored rules
4. **Deduplication** - SHA256 hash prevents duplicate events
5. **AI Fallback** - (Coming soon) Falls back to AI when rule-based parsing fails

### Configured Websites

- **theloft.at** - Cultural venue (regex extraction for text format)
- **grelleforelle.com** - Electronic music venue (CSS selectors)
- **daswerk.org** - Cultural center (CSS selectors with German dates)
- **savedate.io/@prst** - Event platform (CSS selectors with data attributes)

### Scraping API Endpoints

```bash
# Run scraping for all enabled sites
POST /api/scraping/rules/run

# Scrape specific site
POST /api/scraping/rules/site/{siteName}

# View site configurations and stats
GET /api/scraping/rules/status

# Clear cached HTML
DELETE /api/scraping/cache/{siteName}
```

### Managing ScrapeRules

```sql
-- Connect to database
docker exec -it eventfinder_db psql -U eventfinder -d eventfinder

-- View all scrape rules
SELECT site_name, enabled, extraction_mode, ai_enabled FROM scrape_rules;

-- Disable a site
UPDATE scrape_rules SET enabled = false WHERE site_name = 'theloft.at';

-- Update CSS selector for a site
UPDATE scrape_rules SET title_selector = '.new-title-class' WHERE site_name = 'grelleforelle.com';
```

### HTML Storage Details

- **Location (container)**: `/app/scraper-data`
- **Location (local dev)**: `./scraper-data` (if running without Docker)
- **Format**: `{siteName}_{yyyyMMdd}.html`
- **Purpose**: Offline re-parsing, debugging, reduced server load
- **Persistence**: Stored in Docker volume `scraper_data`

---

## 📝 Common Development Tasks

### View Backend Logs
```powershell
docker logs -f eventfinder_backend
```

### View Frontend Logs
```powershell
docker logs -f eventfinder_frontend
```

### Restart a Single Service
```powershell
docker-compose restart backend
# or
docker-compose restart frontend
```

### Rebuild After Dependency Changes
```powershell
docker-compose up --build
```

### Clean Everything and Start Fresh
```powershell
docker-compose down -v  # -v removes volumes (deletes database!)
docker-compose up --build
```

### Access Database Directly
```powershell
docker exec -it eventfinder_db psql -U eventfinder -d eventfinder
```

### Trigger Web Scraping
```powershell
# Scrape all enabled websites using rule-based scraper
curl -X POST http://localhost:8080/api/scraping/rules/run

# Scrape a specific site
curl -X POST http://localhost:8080/api/scraping/rules/site/theloft.at

# Check scraping status for all sites
curl http://localhost:8080/api/scraping/rules/status

# Clear cached HTML for a site (force fresh fetch)
curl -X DELETE http://localhost:8080/api/scraping/cache/theloft.at
```

### Access Cached HTML Files
The scraper stores HTML locally to avoid repeated website hits:
```powershell
# View cached HTML files on your local machine
ls ./scraper-data/

# Or inside the Docker container
docker exec eventfinder_backend ls /app/scraper-data

# Copy cached HTML from container to local
docker cp eventfinder_backend:/app/scraper-data ./local-scraper-data
```

**Note:** HTML files are named `{siteName}_{yyyyMMdd}.html` (e.g., `theloft.at_20260222.html`)

---

## 🐛 Troubleshooting

### Port Already in Use
```powershell
# Find what's using the port
netstat -ano | findstr :8080
# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F
```

### Frontend Can't Reach Backend
1. Check backend is running: http://localhost:8080/api/events
2. Check CORS settings in backend application.yml
3. Verify VITE_API_BASE in docker-compose.yml

### Database Connection Errors
```powershell
# Check if database is ready
docker exec eventfinder_db pg_isready -U eventfinder
```

### Hot Reload Not Working
- Make sure volumes are mounted correctly in docker-compose.yml
- Try restarting the container: `docker-compose restart frontend`

### PWA Not Installing
- PWA requires HTTPS (or localhost)
- Check manifest.json is accessible: http://localhost:5173/manifest.json
- Check service worker: Open DevTools → Application → Service Workers

---

## 🌐 Environment Variables

### Backend (.env or docker-compose.yml)
```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/eventfinder
SPRING_DATASOURCE_USERNAME: eventfinder
SPRING_DATASOURCE_PASSWORD: eventfinder
APP_CORS_ORIGINS: "*"  # Allow all origins for local dev
```

### Frontend (.env.local or docker-compose.yml)
```yaml
VITE_API_BASE: http://localhost:8080
```

**Never commit `.env` files with real credentials!**

---

## 🧹 Cleanup

### Remove All Containers and Images
```powershell
# Stop and remove containers
docker-compose down

# Remove all EventFinder images
docker rmi $(docker images -q eventfinder*)

# Remove dangling images
docker image prune -f
```

### Free Up Disk Space
```powershell
# Remove unused Docker data
docker system prune -a --volumes
```

---

## 🚀 Next Steps

- **Ready to deploy?** See [DEPLOYMENT.md](DEPLOYMENT.md)
- **Want mobile access?** See [MOBILE_DEPLOYMENT.md](MOBILE_DEPLOYMENT.md)
- **Adding features?** Edit code in `frontend/src` or `backend/src`

---

## 📚 Useful Commands Cheat Sheet

```powershell
# Start development
docker-compose up

# Start in background
docker-compose up -d

# View logs
docker-compose logs -f

# Stop everything
docker-compose down

# Rebuild and start
docker-compose up --build

# Test production build
docker-compose -f docker-compose.prod.yml up --build

# Clean restart
docker-compose down -v && docker-compose up --build
```

---

**Happy coding! 🎉**
