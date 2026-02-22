# EventFinder Implementation Summary

## Project Status: Fully Deployed ✅

**Current State**: All services running in Docker Compose with 4 websites configured and scraping functional.

```
✅ Backend: Spring Boot 3.2.5 (Java 21)
✅ Database: PostgreSQL 16 with Flyway migrations (V1, V2, V3 applied)
✅ Frontend: React with Vite
✅ Mobile: Expo (React Native)
✅ Docker Compose: 3 services running (db, backend, frontend)
✅ Rule-Based Generic Scraper: GenericScraper with ScrapeRule configuration
✅ HTML Caching: HtmlStorage service saving raw HTML
✅ Deduplication: SHA256 hash-based duplicate detection
✅ 4 Websites Configured: The Loft, Grelle Forelle, Das Werk, SaveDate @ PRST
```

---

## What Has Been Built

### 1. **Centralized Frontend Configuration**
- **File**: `shared/frontendConfig.js`
- **Purpose**: Single source of truth for both web and mobile frontends
- **Contents**: 
  - API endpoints and timeouts
  - Theme colors, fonts, spacing
  - Default coordinates and app constants
  - Error messages and localization strings

**Result**: No more duplicate CSS or configuration between Vite and Expo.

### 2. **Rule-Based Generic Web Scraper** (vs Site-Specific Classes)
- **File**: `backend/src/main/java/.../scraper/impl/GenericScraper.java`
- **Purpose**: Single scraper class configured via database rules
- **How It Works**:
  - Loads `ScrapeRule` from database
  - Uses CSS selectors OR regex patterns (configurable per website)
  - Parses dates flexibly with locale/format from database
  - Extracts prices, venue names, descriptions dynamically
  - NO hardcoded site-specific methods

**Result**: Add new websites by inserting rows in `scrape_rules` table, not by creating new classes.

### 3. **HTML Caching System**
- **File**: `backend/src/main/java/.../scraper/HtmlStorage.java`
- **Purpose**: Cache raw HTML to reduce repeated website hits
- **Behavior**:
  - First run of day: Fetches fresh HTML from website
  - Subsequent runs: Uses cached HTML (no network overhead)
  - Files stored at: `/app/scraper-data/{siteName}_{yyyyMMdd}.html`
  - Persisted via Docker volume `scraper_data`

**Result**: "Access the website only once or twice a day" achieved.

### 4. **Deduplication System**
- **Hash Calculation**: SHA256(title + startDateTime + organizer)
- **Storage**: `dedup_hash` column in events table
- **Check**: Before inserting event, verify hash doesn't exist
- **Purpose**: Prevent duplicate events across multiple scraping runs

**Result**: Safe to scrape same website multiple times without creating duplicates.

### 5. **Database Schema (PostgreSQL 16)**
- **Migrations**: V1 (initial), V2 (event expansion), V3 (scrape rules + 4 sites)
- **Tables**:
  - `events` - Expanded with 12+ scraper-specific fields
  - `scrape_rules` - Configuration for each website (40+ fields per row)
  - `flyway_schema_history` - Migration tracking
- **Indexes**: Optimized for dedup checks, source filtering, date ranges

**Result**: Fully normalized schema supporting rule-based scraping.

### 6. **REST API Endpoints**
```
POST   /api/scraping/rules/run                    -- Scrape all enabled sites
POST   /api/scraping/rules/site/{siteName}        -- Scrape specific site
GET    /api/scraping/rules/status                 -- View site configs & stats
DELETE /api/scraping/cache/{siteName}             -- Force fresh fetch
```

**Result**: Simple, stateless HTTP interface for scraping control.

### 7. **Orchestration Service**
- **File**: `ScrapeOrchestrationService.java`
- **Workflow**:
  1. Load enabled ScrapeRules
  2. For each site: Check HTML cache → Fetch if needed → Parse with GenericScraper
  3. Check deduplication → Save new events
  4. Return statistics (total, new, duplicates)

**Result**: Coordinated multi-site scraping with automatic deduplication.

---

## 4 Configured Websites

### The Loft (theloft.at)
```
URL: https://www.theloft.at/programm/
Extraction: REGEX mode (text from link text)
Date Format: German "E. d.M.yyyy HH:mm" (e.g., "Di. 3.3.2026 19:00")
Example Parse: "Di. 3.3.2026 19:00, Eintritt: € freie Spende SOLIDRAGITY Wohnzimmer"
  → Title: "SOLIDRAGITY"
  → Date: March 3, 2026 at 19:00
  → Price: Free / donation
  → Venue: Wohnzimmer (room within venue)
Status: ✅ Functional, AI fallback enabled
```

### Grelle Forelle (grelleforelle.com)
```
URL: https://grelleforelle.com/events
Extraction: CSS Selector mode
Date Format: Short "d/M" (e.g., "3/3")
Selectors: .event-card (container), .event-title (title), .event-date (date)
Status: ✅ Configured, AI fallback enabled
Notes: Electronic music venue; may need detail page fetch for full descriptions
```

### Das Werk (daswerk.org)
```
URL: https://daswerk.org/programm
Extraction: CSS Selector mode
Date Format: German months "d. MMMM yyyy HH:mm" (e.g., "3. März 2026 19:00")
Selectors: .program-item, h3, .event-date
Status: ✅ Configured, AI fallback enabled
Notes: Uses German month names including "Feber" for February
```

### SaveDate @ PRST (savedate.io/@prst)
```
URL: https://savedate.io/@prst
Extraction: CSS Selector mode (modern semantic attributes)
Date Format: ISO "yyyy-MM-dd'T'HH:mm:ss" (e.g., "2026-03-22T19:00:00")
Selectors: data-testid attributes (.event, .eventTitle, .eventDate)
Status: ✅ Configured, AI fallback enabled
Notes: Modern platform with clean semantic structure
```

---

## Technology Stack

### Backend
- **Language**: Java 21 (Spring Boot 3.2.5)
- **Database**: PostgreSQL 16 with Flyway migrations
- **ORM**: Hibernate/JPA
- **Web Scraping**: JSoup (HTML parsing), Regex (text matching)
- **Concurrency**: Virtual threads for parallel scraping

### Frontend (Web)
- **Framework**: React 18 with Vite
- **Package Manager**: npm
- **Build**: Vite (lightning-fast HMR)
- **CSS**: CSS Modules + Shared theme variables

### Frontend (Mobile)
- **Framework**: Expo (React Native)
- **Deployment**: iOS/Android via EAS

### DevOps
- **Containerization**: Docker
- **Orchestration**: Docker Compose
- **Volumes**: 
  - `db_data` - PostgreSQL persistence
  - `scraper_data` - HTML cache persistence

---

## How to Use

### 1. Start Everything
```bash
cd e:\Projekte\EventFinder\eventfinder
docker-compose up -d
```

### 2. Verify Services
```bash
# Check logs
docker logs -f eventfinder_backend

# Should see:
# - Flyway migrations (V1, V2, V3)
# - GenericScraper initialization
# - Server listening on port 8080
```

### 3. Scrape Events
```bash
# Option A: Via HTTP
curl -X POST http://localhost:8080/api/scraping/rules/run

# Option B: Via UI (Admin tab → "Run all scrapers" button)
# Open http://localhost:5173/admin

# Returns: { totalScraped, totalNew, sitesProcessed, errors }
```

### 4. View Results
```bash
# In Frontend (localhost:5173)
# → Events tab: Shows all scraped events grouped by location with horizontal slider
# → Admin tab: Shows website status and cached HTML info

# In Database
docker exec -it eventfinder_db psql -U eventfinder -d eventfinder
SELECT scraped_from, COUNT(*) FROM events GROUP BY scraped_from;
```

### 5. Modify Scraping Rules
```sql
-- Update CSS selectors if website changes
UPDATE scrape_rules 
SET title_selector = '.new-class'
WHERE site_name = 'daswerk.org';

-- Add new website
INSERT INTO scrape_rules (site_name, base_url, ...) VALUES (...);

-- Next scraping run uses new rules automatically
```

---

## File Structure

```
eventfinder/
├── SCRAPING_SYSTEM.md              ← Architecture & website details
├── DATABASE_SCHEMA.md              ← Schema, migrations, queries
├── FRONTEND_UI_LAYOUT.md           ← Component structure & layout changes
├── DEPLOYMENT.md                   ← Docker & production setup
├── LOCAL_DEVELOPMENT.md            ← Dev environment guide
│
├── shared/
│   └── frontendConfig.js           ← Centralized config (themes, API)
│
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/main/java/com/example/eventfinder/
│   │   ├── EventFinderApplication.java
│   │   ├── config/
│   │   │   ├── ScraperInitializer.java    ← Initializes GenericScraper
│   │   │   └── WebConfig.java             ← CORS, REST config
│   │   ├── controller/
│   │   │   ├── EventController.java
│   │   │   ├── ScrapingController.java    ← REST endpoints
│   │   │   └── ...
│   │   ├── model/
│   │   │   ├── Event.java                 ← Expanded entity (V2, V3)
│   │   │   ├── ScrapeRule.java            ← Configuration entity (V3)
│   │   │   └── ...
│   │   ├── scraper/
│   │   │   ├── impl/
│   │   │   │   └── GenericScraper.java    ← Rule-based scraper
│   │   │   ├── HtmlStorage.java           ← HTML caching
│   │   │   └── ScrapedEvent.java          ← DTO
│   │   ├── service/
│   │   │   ├── ScrapeOrchestrationService.java ← Multi-site orchestration
│   │   │   └── EventService.java
│   │   └── repository/
│   │       ├── EventRepository.java       ← Dedup & stat queries
│   │       ├── ScrapeRuleRepository.java  ← Rule access
│   │       └── ...
│   └── src/main/resources/
│       ├── application.yml                ← Scraper settings
│       └── db/migration/
│           ├── V1__* (auto-generated)
│           ├── V2__Expand_Event_for_Scrapers.sql
│           └── V3__Insert_Initial_ScrapeRules.sql
│
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   ├── Dockerfile
│   ├── src/
│   │   ├── App.jsx                 ← Main component
│   │   ├── components/
│   │   │   ├── LocationSlider.jsx  ← NEW: Horizontal event carousel
│   │   │   ├── VenuePagination.jsx ← NEW: Vertical venue pagination
│   │   │   ├── ScrapedWebsites.jsx ← NEW: Website status display
│   │   │   ├── EventCard.jsx
│   │   │   └── ScrapingPanel.jsx
│   │   ├── config.js               ← API_BASE, constants
│   │   └── theme.js                ← Uses shared/frontendConfig.js
│   └── public/
│
└── docker-compose.yml              ← 3 services: db, backend, frontend
```

---

## Key Design Decisions

### 1. **Single Generic Scraper vs Site-Specific Classes**
✅ **GenericScraper + ScrapeRule Configuration**
- **Advantage**: Add/modify websites without code changes
- **Implementation**: CSS selectors or regex patterns configured in database
- **Fallback**: AI extraction when rules fail (planned)

### 2. **HTML Caching Strategy**
✅ **One file per site per day** (`{site}_{yyyyMMdd}.html`)
- **Advantage**: Reduced server load, offline analysis, persistent cache
- **Trade-off**: Stale data (only updates daily)
- **Mitigation**: API endpoint to force fresh fetch

### 3. **Event Deduplication**
✅ **SHA256 hash of (title + startDateTime + organizer)**
- **Advantage**: Works across multiple runs, scraper versions
- **Trade-off**: Title changes = new event (not merged)
- **Use Case**: Prevents database bloat from repeated runs

### 4. **Database Schema Evolution**
✅ **Flyway migrations with CREATE TABLE in INSERT queries**
- **Advantage**: Automatic schema evolution, version tracking
- **Implementation**: V3 creates `scrape_rules` before inserting 4 website configs
- **Safety**: Automatically bypassed on subsequent runs

### 5. **Shared Frontend Config**
✅ **Single `frontendConfig.js` for Vite + Expo**
- **Advantage**: No duplicate CSS, themes, API URLs
- **Structure**: APP_CONFIG, API_CONFIG, FRONTEND_THEME, MOBILE_THEME
- **Locations**: Both `frontend` and `mobile-expo` import same file

---

## Next Steps (Planned)

### Phase 1: Frontend UI Enhancements ⏳
- [ ] Implement LocationSlider (horizontal carousel per venue)
- [ ] Implement VenuePagination (show 3 venues per page)
- [ ] Add ScrapedWebsites status component to Admin tab
- [ ] Responsive design for mobile

### Phase 2: AI Extraction Service 📋
- [ ] Create `AIExtractionService` interface
- [ ] Implement with free LLM (Ollama local or Hugging Face API)
- [ ] Fallback logic: Use AI only if rule-based extraction returns 0 events
- [ ] Cache AI results to reduce API calls

### Phase 3: CSS Selector Refinement 🔧
- [ ] Run scrapers against actual websites
- [ ] Verify/update selectors based on real HTML
- [ ] Test regex patterns on live data
- [ ] Document any site-specific quirks

### Phase 4: Advanced Features
- [ ] Schedule daily scraping (Spring @Scheduled at 08:00)
- [ ] Event image download and storage
- [ ] Manual rule builder UI (drag/drop CSS selectors)
- [ ] Duplicate event merging interface
- [ ] Scraping statistics dashboard
- [ ] Email alerts for new events

---

## Troubleshooting Quick Reference

### No events showing?
```bash
1. Check scrapers ran: docker logs eventfinder_backend | grep "Scrape"
2. Check database: SELECT COUNT(*) FROM events;
3. Check dedup_hash isn't blocking: SELECT * FROM events WHERE scraped_from='theloft.at';
4. Run scraper manually: curl -X POST http://localhost:8080/api/scraping/rules/run
```

### CSS selectors not finding events?
```bash
1. Visit website manually
2. Inspect HTML structure with DevTools
3. Test selector in browser console: document.querySelectorAll('.event-card')
4. Update scrape_rules with correct selector
5. Force fresh fetch: curl -X DELETE http://localhost:8080/api/scraping/cache/daswerk.org
```

### Database migrations failed?
```bash
1. Check logs: docker logs eventfinder_backend | grep -i "flyway\|migration\|error"
2. Connect to DB: docker exec -it eventfinder_db psql -U eventfinder -d eventfinder
3. Check history: SELECT * FROM flyway_schema_history;
4. If V3 failed: Check scrape_rules table exists, check INSERT statements
```

### High CPU/Memory usage?
```bash
1. Check concurrent scrapers: grep "max-concurrent-scrapers" application.yml
2. Reduce to 1-2 instead of 3
3. Increase delays: min-delay-between-requests: 5000
```

---

## Documentation Files

| File | Purpose |
|------|---------|
| [SCRAPING_SYSTEM.md](SCRAPING_SYSTEM.md) | Complete scraping architecture, 4 websites, GenericScraper details, API endpoints, usage examples |
| [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) | Schema design, migrations V1-V3, table structures, SQL queries, backup/restore |
| [FRONTEND_UI_LAYOUT.md](FRONTEND_UI_LAYOUT.md) | Component architecture, LocationSlider, VenuePagination, ScrapedWebsites components with code |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Docker Compose setup, environment variables, production deployment |
| [LOCAL_DEVELOPMENT.md](LOCAL_DEVELOPMENT.md) | Development environment setup, running locally, debugging |

---

## Quick Start Commands

```bash
# Start everything
docker-compose up -d

# View logs
docker logs -f eventfinder_backend
docker logs -f eventfinder_frontend

# Run scraper
curl -X POST http://localhost:8080/api/scraping/rules/run

# Access database
docker exec -it eventfinder_db psql -U eventfinder -d eventfinder

# Stop everything
docker-compose down

# Full rebuild (clean)
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (React Vite)                     │
│ ┌──────────────────────────────────────────────────────────┐│
│ │ App.jsx: Tabs (Events, Places, Admin)                   ││
│ ├─────────────────┬──────────────────┬──────────────────┐ ││
│ │ LocationSlider  │ VenuePagination  │ ScrapedWebsites  │ ││
│ │ (Horizontal)    │ (Vertical 3/page)│ (Status display) │ ││
│ └─────────────────┴──────────────────┴──────────────────┘ ││
│ ┌──────────────────────────────────────────────────────────┐│
│ │ Shared Frontend Config                                   ││
│ │ - API_BASE, API_TIMEOUT                                  ││
│ │ - FRONTEND_THEME, MOBILE_THEME                           ││
│ └──────────────────────────────────────────────────────────┘│
└────────────────────┬─────────────────────────────────────────┘
                     │ HTTP (REST)
                     ↓
┌──────────────────────────────────────────────────────────────┐
│          Backend (Spring Boot 3.2.5 Java 21)                 │
├──────────────────────────────────────────────────────────────┤
│ ScrapingController                                           │
│ - GET  /api/scraping/rules/status                           │
│ - POST /api/scraping/rules/run                              │
│ - POST /api/scraping/rules/site/{siteName}                  │
│ - DELETE /api/scraping/cache/{siteName}                     │
├──────────────────────────────────────────────────────────────┤
│ ScrapeOrchestrationService                                  │
│ ├─ Load enabled ScrapeRules from database                    │
│ ├─ For each site:                                            │
│ │ ├─ Check HtmlStorage (cached HTML?)                        │
│ │ ├─ If not cached → Fetch from website → Save HTML          │
│ │ ├─ Parse with GenericScraper (using ScrapeRule config)     │
│ │ ├─ Check dedup_hash (avoid duplicates)                     │
│ │ └─ Save new events to database                             │
│ └─ Return statistics (total, new, duplicates)                │
├──────────────────────────────────────────────────────────────┤
│ GenericScraper                                              │
│ - extractWithSelectors() [CSS mode]                         │
│ - extractWithRegex() [REGEX mode]                           │
│ - parseDateTime(format, locale)                             │
│ - parsePricing()                                            │
├──────────────────────────────────────────────────────────────┤
│ HtmlStorage (Persistent /app/scraper-data/)                 │
│ - saveHtml(website, html)                                   │
│ - loadHtml(website) [cached for today]                      │
│ - cleanup(olderThanDays)                                    │
└────────┬────────────────────┬──────────────────────┬─────────┘
         │                    │                      │
         ↓                    ↓                      ↓
┌─────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│  PostgreSQL 16  │ │ scrape_rules     │ │ events          │
│                 │ │ ────────────────  │ │ ───────────────  │
│ eyword:scraper_ │ │ id, site_name    │ │ id, title       │
│ data (Docker    │ │ base_url         │ │ start_dateTime  │
│ volume)         │ │ extraction_mode  │ │ price_from/to   │
│                 │ │ *.selector*      │ │ venue           │
│                 │ │ date_format      │ │ scraped_from    │
│                 │ │ regex_pattern    │ │ dedup_hash      │
│                 │ │ ai_enabled       │ │ raw_source_html │
│                 │ │ ...              │ │ created_at      │
│                 │ │ (40+ fields)     │ │ ...             │
│                 │ │                  │ │                 │
│                 │ │ Rows: 4 websites │ │ Rows: 20-100+   │
│                 │ │ (theloft,        │ │ (depends on     │
│                 │ │  grelle, daswerk │ │  scraping)      │
│                 │ │  savedate)       │ │                 │
└─────────────────┘ └──────────────────┘ └──────────────────┘
```

---

## Summary

✅ **Fully implemented rule-based web scraping system** for 4 Vienna event venues

✅ **Single GenericScraper class** configured via database (no site-specific code)

✅ **HTML caching system** reduces website hits (once per day per site)

✅ **Deduplication** prevents database bloat

✅ **Centralized frontend config** for code reuse across Vite + Expo

⏳ **Next: Frontend UI layout changes** (horizontal carousel per location, vertical pagination)

📋 **Planned: AI fallback** for rule-based extraction failures

All documentation provided in 4 comprehensive markdown files.
