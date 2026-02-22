# EventFinder Web Scraping System

## Overview

EventFinder implements a **rule-based generic web scraper** that extracts events from multiple Vienna cultural venues without requiring site-specific code changes. The system uses **database-driven configurations** to handle different website structures, with **HTML caching** to reduce server load and support offline re-parsing.

---

## Implemented Websites

### 1. **The Loft** (theloft.at)
- **URL**: https://www.theloft.at/programm/
- **Type**: Independent cultural venue in Vienna
- **Extraction Method**: REGEX (text-based)
- **Event Format**: Link text contains all info: "Di. 3.3.2026 19:00, Eintritt: € freie Spende SOLIDRAGITY Wohnzimmer"
- **Parsing**: Regex extracts date, price, title, and venue from the link text
- **Status**: ✅ Configured with AI fallback enabled

### 2. **Grelle Forelle** (grelleforelle.com)
- **URL**: https://grelleforelle.com/events
- **Type**: Electronic music venue, Berlin/Vienna
- **Extraction Method**: CSS Selector
- **Event Format**: HTML cards with separate title, date, price, location elements
- **Parsing**: CSS selectors target `.event-card`, `.event-title`, `.event-date`, etc.
- **Status**: ✅ Configured with AI fallback enabled

### 3. **Das Werk** (daswerk.org)
- **URL**: https://daswerk.org/programm
- **Type**: Cultural center, Vienna
- **Extraction Method**: CSS Selector
- **Event Format**: HTML list/grid with German date names (Feber, März, etc.)
- **Parsing**: CSS selectors with German locale date format parsing
- **Status**: ✅ Configured with AI fallback enabled

### 4. **SaveDate @ PRST** (savedate.io/@prst)
- **URL**: https://savedate.io/@prst
- **Type**: Modern event platform (PRST is an event organizer)
- **Extraction Method**: CSS Selector with data attributes
- **Event Format**: HTML with `data-testid` attributes and ISO date format
- **Parsing**: Modern semantic HTML attributes with ISO-8601 date parsing
- **Status**: ✅ Configured with AI fallback enabled

---

## Architecture

### Database Schema

#### `scrape_rules` Table
Stores configuration for each website's scraping rules.

```sql
CREATE TABLE scrape_rules (
    id BIGSERIAL PRIMARY KEY,
    site_name VARCHAR(255) NOT NULL UNIQUE,  -- e.g., "theloft.at"
    base_url VARCHAR(255) NOT NULL,
    
    -- CSS Selectors for element extraction
    event_list_selector VARCHAR(1000),       -- Container holding all events
    event_item_selector VARCHAR(1000),       -- Individual event element
    title_selector VARCHAR(500),
    date_selector VARCHAR(500),
    price_selector VARCHAR(500),
    venue_selector VARCHAR(500),
    link_selector VARCHAR(500),
    image_selector VARCHAR(500),
    description_selector VARCHAR(500),
    
    -- Date parsing configuration
    date_format VARCHAR(100),                 -- e.g., "E. d.M.yyyy HH:mm"
    date_locale VARCHAR(100),                 -- e.g., "de-DE"
    
    -- Extraction strategy
    extraction_mode VARCHAR(50),              -- CSS_SELECTOR, REGEX, AI_FALLBACK
    event_text_pattern VARCHAR(1000),        -- Regex pattern for text extraction
    
    -- Default metadata
    category VARCHAR(100),                    -- Default category for events
    organizer VARCHAR(100),                   -- Default organizer
    
    -- Advanced options
    requires_detail_page_fetch BOOLEAN,
    detail_page_selector VARCHAR(500),
    ai_enabled BOOLEAN,                       -- Enable AI fallback
    ai_prompt VARCHAR(500),                   -- Custom AI extraction prompt
    rate_limit_ms INTEGER,                    -- Custom rate limit
    
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    notes VARCHAR(2000)                       -- Admin documentation
);
```

#### `events` Table (Extended)
Original table expanded with scraper-specific fields.

```sql
ALTER TABLE events ADD COLUMN IF NOT EXISTS price_from DECIMAL(10, 2);
ALTER TABLE events ADD COLUMN IF NOT EXISTS price_to DECIMAL(10, 2);
ALTER TABLE events ADD COLUMN IF NOT EXISTS price_note VARCHAR(500);
ALTER TABLE events ADD COLUMN IF NOT EXISTS venue VARCHAR(500);
ALTER TABLE events ADD COLUMN IF NOT EXISTS is_recurring BOOLEAN;
ALTER TABLE events ADD COLUMN IF NOT EXISTS recurring_pattern VARCHAR(100);
ALTER TABLE events ADD COLUMN IF NOT EXISTS scraped_from VARCHAR(100);  -- e.g., "theloft.at"
ALTER TABLE events ADD COLUMN IF NOT EXISTS parser_version VARCHAR(50); -- e.g., "GenericScraper_v1"
ALTER TABLE events ADD COLUMN IF NOT EXISTS dedup_hash VARCHAR(64);     -- SHA256 for deduplication
ALTER TABLE events ADD COLUMN IF NOT EXISTS raw_source_html VARCHAR(50000); -- Store HTML for offline analysis
ALTER TABLE events ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE events ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Junction table for event tags
CREATE TABLE event_tags (
    event_id BIGINT NOT NULL,
    tag VARCHAR(100) NOT NULL,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, tag)
);
```

---

## How It Works

### 1. Scraping Flow

```
User Request
    ↓
ScrapeOrchestrationService.scrapeAll()
    ↓
For each enabled ScrapeRule:
    ├─ Check HtmlStorage (cached HTML for today?)
    ├─ If cached → Use cached HTML (skip network request)
    ├─ If not cached → Fetch website → Save to HtmlStorage
    ├─ GenericScraper.scrapeWithRule(rule, html)
    │   ├─ If CSS_SELECTOR mode:
    │   │   └─ extractWithSelectors() - Uses CSS selectors to find elements
    │   ├─ If REGEX mode:
    │   │   └─ extractWithRegex() - Matches text patterns
    │   ├─ parseDateTime() - Parse dates with rule's format/locale
    │   ├─ parsePricing() - Extract price ranges (€8-15, frei, etc.)
    │   └─ convertToEvent() - Create Event entity with metadata
    ├─ Check deduplicationHash (avoid duplicate events)
    └─ Save to database
    ↓
Return statistics (total, new, duplicates, errors)
```

### 2. HTML Caching System

**Purpose**: Avoid repeated website hits, enable offline analysis

**Files**: Stored at `/app/scraper-data/{siteName}_{yyyyMMdd}.html`

**Persistence**: Docker volume `scraper_data` preserves cache between restarts

**Behavior**:
- First run of the day: Fetches fresh HTML from website
- Subsequent runs: Uses cached HTML (no network overhead)
- Cache cleared: Automatically on next day OR via API

### 3. Deduplication

**Method**: SHA256 hash of (title + startDateTime + organizer)

**Purpose**: Prevent duplicate events when scraping the same website multiple times

**How It Works**:
1. GenericScraper calculates hash for each event: `calculateDeduplicationHash(title, dateTime, organizer)`
2. Before saving, check if `dedup_hash` exists in database
3. If exists → Skip event (already in database)
4. If new → Insert event with hash

---

## API Endpoints

### Scraping Control

#### Run All Configured Sites
```bash
POST /api/scraping/rules/run
```
**Response**:
```json
{
  "totalScraped": 45,
  "totalNew": 12,
  "sitesProcessed": 4,
  "siteResults": {
    "theloft.at": 8,
    "grelleforelle.com": 2,
    "daswerk.org": 1,
    "savedate.io/@prst": 1
  },
  "errors": [],
  "timestamp": "2026-02-22T14:45:00"
}
```

#### Scrape Specific Site
```bash
POST /api/scraping/rules/site/{siteName}
# Example: POST /api/scraping/rules/site/theloft.at
```

#### Get Site Status
```bash
GET /api/scraping/rules/status
```
**Response**:
```json
[
  {
    "siteName": "theloft.at",
    "enabled": true,
    "baseUrl": "https://www.theloft.at/programm/",
    "extractionMode": "REGEX",
    "aiEnabled": true,
    "hasCachedHtml": true,
    "eventCount": 23
  },
  ...
]
```

#### Clear Cached HTML
```bash
DELETE /api/scraping/cache/{siteName}
# Example: DELETE /api/scraping/cache/theloft.at
```

---

## Managing ScrapeRules

### View Current Rules

```sql
-- Connect to database
docker exec -it eventfinder_db psql -U eventfinder -d eventfinder

-- List all rules
SELECT site_name, enabled, extraction_mode, ai_enabled, notes FROM scrape_rules;

-- View detailed configuration for one site
SELECT * FROM scrape_rules WHERE site_name = 'theloft.at';
```

### Update CSS Selectors

If website structure changes, update selectors without redeploying:

```sql
UPDATE scrape_rules 
SET title_selector = '.new-selector-class',
    updated_at = CURRENT_TIMESTAMP
WHERE site_name = 'daswerk.org';
```

### Enable/Disable Site

```sql
-- Disable a site (won't be scraped)
UPDATE scrape_rules SET enabled = false WHERE site_name = 'theloft.at';

-- Re-enable later
UPDATE scrape_rules SET enabled = true WHERE site_name = 'theloft.at';
```

### Change Extraction Mode

```sql
-- Switch from REGEX to CSS_SELECTOR
UPDATE scrape_rules 
SET extraction_mode = 'CSS_SELECTOR',
    event_item_selector = '.event-card',
    title_selector = '.event-title',
    updated_at = CURRENT_TIMESTAMP
WHERE site_name = 'theloft.at';
```

### Add New Website

```sql
INSERT INTO scrape_rules (
    site_name, base_url,
    event_item_selector, title_selector, date_selector, price_selector,
    date_format, date_locale,
    extraction_mode, category, organizer,
    enabled, ai_enabled,
    notes, created_at
) VALUES (
    'newsite.at',
    'https://newsite.at/events',
    '.event',  -- CSS selector for event container
    '.event-title',
    '.event-date',
    '.event-price',
    'd.M.yyyy HH:mm',
    'de-DE',
    'CSS_SELECTOR',
    'Music',
    'New Site',
    true,
    true,
    'Notes about this site',
    CURRENT_TIMESTAMP
);
```

---

## Extraction Modes

### CSS Selector Mode (Default)

**Best for**: Websites with clean HTML structure (divs, classes, IDs for events)

**How it works**:
1. Finds all elements matching `event_item_selector`
2. Within each element, extracts sub-elements using:
   - `title_selector`
   - `date_selector`
   - `price_selector`
   - etc.

**Example configuration**:
```javascript
{
  eventItemSelector: '.event-card',
  titleSelector: '.event-card__title',
  dateSelector: '.event-card__date',
  priceSelector: '.event-card__price'
}
```

### Regex Mode

**Best for**: Websites where events are in text/plain format (link text, pre tags)

**How it works**:
1. Extracts raw text from page
2. Matches regex pattern with named/numbered groups:
   - Group 1: Title
   - Group 2: Date
   - Group 3: Price
   - Group 4: Venue

**Example pattern** (for The Loft):
```regex
([A-Za-z]{2}\.\s+\d{1,2}\.\d{1,2}\.\d{4}\s+\d{1,2}:\d{2})
,?\s*(?:Eintritt:\s*)?(.*?)\s+([A-ZÄÖÜ][^\s]+)\s+(Wohnzimmer|Oben|Unten)
```

---

## Backend Services

### ScrapeOrchestrationService
**Location**: `backend/src/main/java/.../service/ScrapeOrchestrationService.java`

**Responsibilities**:
- Load enabled ScrapeRules
- Check HtmlStorage for cached pages
- Call GenericScraper with rules
- Handle deduplication
- Save events to database
- Return scraping statistics

**Key Methods**:
- `scrapeAll()` - Scrape all enabled sites
- `scrapeBySiteName(String siteName)` - Scrape single site
- `getSiteStatus()` - View configuration and stats

### GenericScraper
**Location**: `backend/src/main/java/.../scraper/impl/GenericScraper.java`

**Responsibilities**:
- Accept ScrapeRule configuration
- Fetch or parse HTML
- Extract events using CSS selectors or regex
- Parse dates with locale support
- Parse pricing information
- Create Event entities with metadata

**Key Methods**:
- `scrapeWithRule(ScrapeRule)` - Fetch and scrape
- `scrapeWithRule(ScrapeRule, String html)` - Scrape cached HTML
- `extractWithSelectors(Document, ScrapeRule, String html)` - CSS-based extraction
- `extractWithRegex(Document, ScrapeRule, String html)` - Regex-based extraction
- `parseDateTime(String, String format, String locale)` - Flexible date parsing
- `parsePricing(ScrapedEvent, String priceStr)` - Price extraction

### HtmlStorage Service
**Location**: `backend/src/main/java/.../scraper/HtmlStorage.java`

**Responsibilities**:
- Save raw HTML to disk
- Load cached HTML
- Check if cache exists for today
- Cleanup old files

**Storage Path**: `/app/scraper-data` (configurable via `scraper.html-storage-path`)

**File Format**: `{siteName}_{yyyyMMdd}.html`

---

## Pricing Parser

The `parsePricing()` method handles various price formats:

```
"Frei"              → priceFrom: 0
"€ freie Spende"    → priceFrom: 0
"ab € 14,70"        → priceFrom: 14.70
"€ 15"              → priceFrom: 15.00
"€ 8/10"            → priceFrom: 8.00, priceTo: 10.00
"€ 1/6/9/12"        → priceFrom: 1.00, priceTo: 12.00
"unknown format"    → priceNote: "unknown format"
```

---

## Date Parsing

The `parseDateTime()` method is flexible:

**Supports**:
- German formats: `d.M.yyyy HH:mm`, `E. d.M.yyyy HH:mm`
- English: `MM/dd/yyyy`, `yyyy-MM-dd'T'HH:mm:ss` (ISO)
- Custom formats via `date_format` in ScrapeRule
- Locale-aware parsing (de-DE, en-US, etc.)

**Example**:
- Input: "Di. 3.3.2026 19:00" with format `E. d.M.yyyy HH:mm` and locale `de-DE`
- Result: `LocalDateTime(2026, 3, 3, 19, 0, 0)`

---

## Deduplication Strategy

Events are deduplicated using a SHA256 hash:

```java
hash = SHA256(title + "|" + startDateTime + "|" + organizer)
```

**Why this approach**:
- Handles multiple scraping runs (no duplicates)
- Resistant to minor HTML changes
- Survives website CSS reorganization
- Can update events if metadata changes

**Database check**:
```sql
SELECT * FROM events WHERE dedup_hash = 'xxx...';
```

---

## Configuration

### Application Settings

**File**: `backend/src/main/resources/application.yml`

```yaml
scraper:
  enabled: true
  min-delay-between-requests: 2000    # Wait 2s between website requests
  max-delay-between-requests: 5000    # Random 2-5s delay (politeness)
  connection-timeout: 10000           # 10 seconds to connect
  max-retries: 3                      # Retry failed requests
  max-concurrent-scrapers: 3          # Max 3 parallel scraping jobs
  follow-redirects: true              # Handle HTTP redirects
  user-agent: "Mozilla/5.0 ..."       # Browser user-agent
  html-storage-path: ./scraper-data   # Where to save HTML files

spring:
  flyway:
    enabled: true                     # Auto-run database migrations
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate              # Validate schema (don't auto-modify)
```

---

## Troubleshooting

### Sites Not Being Scraped

1. **Check if enabled**:
   ```sql
   SELECT site_name, enabled FROM scrape_rules;
   ```

2. **Check for errors**:
   ```bash
   docker logs eventfinder_backend | grep -i scrape
   ```

3. **Check if cached HTML is stale**:
   ```bash
   docker exec eventfinder_backend ls -lah /app/scraper-data/
   # Should have files dated today
   ```

### No Events Found

1. **Verify CSS selectors are correct**:
   - Visit the website manually
   - Use browser DevTools to inspect element structure
   - Update `event_item_selector`, etc. in database

2. **Check if extraction mode is appropriate**:
   - View source: `curl https://theloft.at/programm/ > output.html`
   - If events are plain text/links → Use REGEX
   - If events are HTML elements → Use CSS_SELECTOR

3. **Check date parsing**:
   ```bash
   # Look for "Failed to parse date" warnings
   docker logs eventfinder_backend | grep "Failed to parse"
   ```

### Duplicate Events

1. **Dedup is working** if you see same event in multiple runs but only one in database
2. **Disable dedup temporarily** (not recommended):
   - For debugging: Comment out dedup check in ScrapeOrchestrationService
   - Remove old duplicates: `DELETE FROM events WHERE dedup_hash IN (...)`

---

## Next Steps: AI Fallback

When rule-based extraction fails, AI can extract events from HTML:

**Planned Implementation**:
- `AIExtractionService` interface with free LLM backend (Ollama, Hugging Face API)
- When `rule.ai_enabled == true` and extraction returns 0 events
- Send page HTML to LLM with prompt: "Extract all events from this HTML..."
- LLM returns structured JSON with title, date, price, venue
- Fall back to AI only when needed (reduces AI costs)

---

## Future Enhancements

- [ ] Schedule daily scraping (Spring Scheduler at 08:00)
- [ ] Email alerts for new events
- [ ] Manual rule builder UI (drag/drop CSS selectors)
- [ ] Event image download and storage
- [ ] Duplicate event merging UI
- [ ] Scraping statistics dashboard
- [ ] AI extraction with free LLM
