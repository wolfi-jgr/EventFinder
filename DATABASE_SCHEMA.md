# EventFinder Database Schema

## Overview

EventFinder uses **PostgreSQL 16** with **Flyway** database migrations. The schema supports event scraping from multiple Vienna cultural venues with configurable extraction rules, HTML caching, and deduplication.

**Current Version**: V3 (3 migrations applied)

---

## Migration History

### V1 - Initial Schema
Created by Spring Boot JPA Hibernate (auto-generated on first run)

**Tables**:
- `events` - Base event table with title, date, location, description

### V2 - Event Expansion for Scrapers
**File**: `backend/src/main/resources/db/migration/V2__Expand_Event_for_Scrapers.sql`

Added scraper-specific fields to the events table:

```sql
ALTER TABLE events ADD COLUMN price_from DECIMAL(10, 2);           -- Price range start
ALTER TABLE events ADD COLUMN price_to DECIMAL(10, 2);             -- Price range end
ALTER TABLE events ADD COLUMN price_note VARCHAR(500);             -- "freie Spende", etc.
ALTER TABLE events ADD COLUMN venue VARCHAR(500);                  -- Specific venue/stage name
ALTER TABLE events ADD COLUMN is_recurring BOOLEAN DEFAULT FALSE;  -- Recurring event?
ALTER TABLE events ADD COLUMN recurring_pattern VARCHAR(100);      -- Pattern: "weekly Mon", etc.
ALTER TABLE events ADD COLUMN scraped_from VARCHAR(100);           -- Source: "theloft.at", etc.
ALTER TABLE events ADD COLUMN parser_version VARCHAR(50);          -- "GenericScraper_v1"
ALTER TABLE events ADD COLUMN dedup_hash VARCHAR(64);              -- SHA256 for dedup checking
ALTER TABLE events ADD COLUMN raw_source_html VARCHAR(50000);      -- Cache raw HTML for re-parsing
ALTER TABLE events ADD COLUMN created_at TIMESTAMP;                -- When scraper created it
ALTER TABLE events ADD COLUMN updated_at TIMESTAMP;                -- When last updated

-- Indexes for query performance
CREATE INDEX idx_event_dedup_hash ON events(dedup_hash);     -- Fast duplicate check
CREATE INDEX idx_event_scraped_from ON events(scraped_from); -- Filter by source
CREATE INDEX idx_event_created_at ON events(created_at);     -- Timeline queries
CREATE INDEX idx_event_is_recurring ON events(is_recurring);  -- Recurring events search
```

### V3 - Scrape Rules Configuration
**File**: `backend/src/main/resources/db/migration/V3__Insert_Initial_ScrapeRules.sql`

Created table for rule-based scraper configuration + inserted 4 website configs:

```sql
CREATE TABLE scrape_rules (
    id BIGSERIAL PRIMARY KEY,
    site_name VARCHAR(255) UNIQUE NOT NULL,
    base_url VARCHAR(255) NOT NULL,
    
    -- CSS Selectors (empty for REGEX mode sites)
    event_list_selector VARCHAR(1000),
    event_item_selector VARCHAR(1000),
    title_selector VARCHAR(500),
    date_selector VARCHAR(500),
    price_selector VARCHAR(500),
    venue_selector VARCHAR(500),
    link_selector VARCHAR(500),
    image_selector VARCHAR(500),
    description_selector VARCHAR(500),
    
    -- Regex mode
    event_text_pattern VARCHAR(1000),
    
    -- Date parsing
    date_format VARCHAR(100),
    date_locale VARCHAR(100),
    
    -- Extraction strategy
    extraction_mode VARCHAR(50),  -- CSS_SELECTOR | REGEX | AI_FALLBACK
    
    -- Default metadata
    category VARCHAR(100),
    organizer VARCHAR(100),
    
    -- Advanced options
    requires_detail_page_fetch BOOLEAN,
    detail_page_selector VARCHAR(500),
    ai_enabled BOOLEAN DEFAULT FALSE,
    ai_prompt VARCHAR(500),
    rate_limit_ms INTEGER,
    
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    notes VARCHAR(2000)
);

-- Initial 4 website configurations inserted
-- See section below for details
```

---

## Current Tables

### `events` Table

Stores scraped (and manually added) events.

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| id | BIGSERIAL | NO | Primary key, auto-increment |
| title | VARCHAR(255) | NO | Event name |
| start_date_time | TIMESTAMP | YES | When event starts |
| end_date_time | TIMESTAMP | YES | When event ends |
| description | TEXT | YES | Event details/description |
| location | VARCHAR(255) | YES | Address or venue name |
| **price_from** | DECIMAL(10,2) | YES | Lowest ticket price (€) |
| **price_to** | DECIMAL(10,2) | YES | Highest ticket price (€) |
| **price_note** | VARCHAR(500) | YES | Text: "freie Spende", "Eintritt frei", etc. |
| **venue** | VARCHAR(500) | YES | Stage/specific room within venue |
| **is_recurring** | BOOLEAN | YES | Is this a recurring event? |
| **recurring_pattern** | VARCHAR(100) | YES | Pattern description |
| **scraped_from** | VARCHAR(100) | YES | Source website: "theloft.at" |
| **parser_version** | VARCHAR(50) | YES | Which scraper created it |
| **dedup_hash** | VARCHAR(64) | YES | SHA256(title + datetime + organizer) |
| **raw_source_html** | VARCHAR(50000) | YES | Original HTML snippet for offline re-parsing |
| **created_at** | TIMESTAMP | YES | When event was added to DB |
| **updated_at** | TIMESTAMP | YES | When event was last modified |

**Indexes**:
- `idx_event_dedup_hash` - Speeds up duplicate checking
- `idx_event_scraped_from` - Filter by source website
- `idx_event_created_at` - Timeline queries
- `idx_event_is_recurring` - Find recurring events

**Row Count**: Typically 20-100+ after first scraping run

---

### `scrape_rules` Table

Configuration for how to scrape each website. One row per configured website.

| Column | Type | Notes |
|--------|------|-------|
| **id** | BIGSERIAL | Primary key |
| **site_name** | VARCHAR(255) | Unique identifier: "theloft.at", "grelleforelle.com", etc. |
| **base_url** | VARCHAR(255) | Starting URL: "https://theloft.at/programm/" |
| **event_list_selector** | VARCHAR(1000) | CSS: Container holding all events (optional) |
| **event_item_selector** | VARCHAR(1000) | CSS: Single event element: ".event-card", "article", etc. |
| **title_selector** | VARCHAR(500) | CSS: Where title is: "h2", ".event-title", etc. |
| **date_selector** | VARCHAR(500) | CSS: Where date is |
| **price_selector** | VARCHAR(500) | CSS: Where price is |
| **venue_selector** | VARCHAR(500) | CSS: Where venue/stage name is |
| **link_selector** | VARCHAR(500) | CSS: Where detail link is (optional) |
| **image_selector** | VARCHAR(500) | CSS: Where event image is (optional) |
| **description_selector** | VARCHAR(500) | CSS: Where description is |
| **event_text_pattern** | VARCHAR(1000) | Regex: Pattern for extracting event data (for REGEX mode) |
| **date_format** | VARCHAR(100) | Format string: "d.M.yyyy HH:mm", "yyyy-MM-dd'T'HH:mm:ss", etc. |
| **date_locale** | VARCHAR(100) | Locale: "de-DE", "en-US", etc. for parsing month names |
| **extraction_mode** | VARCHAR(50) | "CSS_SELECTOR", "REGEX", "AI_FALLBACK" |
| **category** | VARCHAR(100) | Default event category: "Music", "Culture", "Theater" |
| **organizer** | VARCHAR(100) | Default organizer name |
| **requires_detail_page_fetch** | BOOLEAN | Need to fetch detail page? |
| **detail_page_selector** | VARCHAR(500) | CSS: How to find detail link |
| **ai_enabled** | BOOLEAN | Use AI fallback if rules fail? |
| **ai_prompt** | VARCHAR(500) | Custom prompt for AI extraction |
| **rate_limit_ms** | INTEGER | Custom delay between requests (ms) |
| **enabled** | BOOLEAN | Include in scraping runs? |
| **created_at** | TIMESTAMP | When this rule was added |
| **updated_at** | TIMESTAMP | When this rule was last edited |
| **notes** | VARCHAR(2000) | Documentation: selectors, issues, etc. |

**Current Rows**: 4 websites configured

---

## Configured Websites (V3 Migration)

### 1. The Loft (`theloft.at`)

```sql
INSERT INTO scrape_rules (
    site_name, base_url,
    date_format, date_locale,
    extraction_mode,
    event_text_pattern,
    category, organizer,
    ai_enabled,
    notes,
    enabled, created_at
) VALUES (
    'theloft.at',
    'https://www.theloft.at/programm/',
    'E. d.M.yyyy HH:mm',  -- "Di. 3.3.2026 19:00"
    'de-DE',
    'REGEX',
    -- Matches: "Di. 3.3.2026 19:00, Eintritt: € freie Spende SOLIDRAGITY Wohnzimmer"
    '([A-Za-z]{2}\.\s+\d{1,2}\.\d{1,2}\.\d{4}\s+\d{1,2}:\d{2}).*?Eintritt:\s*(.*?)\s+([A-ZÄÖÜ][^\s]+)\s+(Wohnzimmer|Oben|Unten|Keller|Dachboden)',
    'Event',
    'The Loft',
    true,
    'Events in link text. Venues: Wohnzimmer, Oben, Unten, Keller, Dachboden. Uses German date format with day abbreviations.',
    true,
    CURRENT_TIMESTAMP
);
```

**How it works**:
1. Fetches HTML from https://www.theloft.at/programm/
2. Extracts all links (`<a>` tags)
3. Applies regex to link text
4. Regex captures: date group (1), price group (2), title group (3), venue group (4)
5. Parses date as German format with day of week
6. If regex fails → AI fallback (enabled)

---

### 2. Grelle Forelle (`grelleforelle.com`)

```sql
INSERT INTO scrape_rules (
    site_name, base_url,
    event_item_selector, title_selector, date_selector, price_selector, venue_selector,
    date_format, date_locale,
    extraction_mode,
    category, organizer,
    ai_enabled,
    notes,
    enabled, created_at
) VALUES (
    'grelleforelle.com',
    'https://grelleforelle.com/events',
    '.event-card',          -- Event container
    '.event-title, h3',     -- Title (multiple selectors)
    '.event-date, .date',   -- Date
    '.event-price, .price', -- Price
    '.event-venue, .venue', -- Venue
    'd/M',                  -- Short date format "3/3" for March 3rd
    'de-DE',
    'CSS_SELECTOR',
    'Music',
    'Grelle Forelle',
    true,
    'Electronic music venue (Berlin/Vienna). Basic CSS selector extraction. May need detail page fetch for full descriptions.',
    true,
    CURRENT_TIMESTAMP
);
```

**How it works**:
1. Fetches HTML from https://grelleforelle.com/events
2. Finds all elements matching `.event-card` (event containers)
3. Within each `.event-card`:
   - `.event-title` or `h3` → title
   - `.event-date` or `.date` → date string
   - `.event-price` or `.price` → price
   - `.event-venue` or `.venue` → venue
4. Parses date as "d/M" (short format, assumes current year, requires adding year)
5. If extraction returns 0 events → AI fallback

---

### 3. Das Werk (`daswerk.org`)

```sql
INSERT INTO scrape_rules (
    site_name, base_url,
    event_item_selector, title_selector, date_selector,
    date_format, date_locale,
    extraction_mode,
    category, organizer,
    ai_enabled,
    notes,
    enabled, created_at
) VALUES (
    'daswerk.org',
    'https://daswerk.org/programm',
    '.program-item, .event',  -- Event container
    'h3, h2, .event-title',   -- Title
    '.event-date, .date, span', -- Date
    'd. MMMM yyyy HH:mm',     -- "3. März 2026 19:00" (German month names)
    'de-DE',
    'CSS_SELECTOR',
    'Culture',
    'Das Werk',
    true,
    'Cultural center Vienna. Uses German month names (März, Feber, etc.). May need locale-aware date parsing.',
    true,
    CURRENT_TIMESTAMP
);
```

**Special Note**: German date format uses "Feber" for February (Austrian/German dialect), which LibreTime's LocalizedText handles.

---

### 4. SaveDate @ PRST (`savedate.io/@prst`)

```sql
INSERT INTO scrape_rules (
    site_name, base_url,
    event_item_selector, title_selector, date_selector, price_selector,
    date_format, date_locale,
    extraction_mode,
    category, organizer,
    ai_enabled,
    notes,
    enabled, created_at
) VALUES (
    'savedate.io/@prst',
    'https://savedate.io/@prst',
    '[data-testid="event"]',    -- Modern semantic attribute
    '[data-testid="eventTitle"]', -- Title from data attribute
    '[data-testid="eventDate"]',   -- Date from data attribute
    '[data-testid="eventPrice"]',  -- Price from data attribute
    'yyyy-MM-dd''T''HH:mm:ss',   -- ISO format "2026-03-22T19:00:00"
    'en-US',
    'CSS_SELECTOR',
    'Event',
    'PRST',
    true,
    'Modern event platform with semantic data-testid attributes. ISO date format. Minimal parsing needed.',
    true,
    CURRENT_TIMESTAMP
);
```

---

## Data Relationships

```
┌─────────────────────┐
│   scrape_rules      │
├─────────────────────┤
│ id (PK)             │
│ site_name (UNIQUE)  │
│ base_url            │
│ extraction_mode     │
│ date_format         │
│ date_locale         │
│ CSS selectors...    │
│ AI settings...      │
└─────────────────────┘
         │
         │ "configuration for"
         │
         ↓
┌─────────────────────┐
│     GenericScraper  │
│   (Java service)    │
└─────────────────────┘
         │
         │ "uses rule to extract"
         │
         ↓
┌─────────────────────┐
│      events         │
├─────────────────────┤
│ id (PK)             │
│ title               │
│ start_date_time     │
│ location            │
│ price_from          │
│ price_to            │
│ venue               │
│ scraped_from  ──────┼──→ (foreign key to scrape_rules.site_name)
│ dedup_hash          │
│ raw_source_html     │
│ created_at          │
└─────────────────────┘
```

---

## Common Queries

### Check What's Configured

```sql
SELECT site_name, enabled, extraction_mode, base_url 
FROM scrape_rules;
```

### View Events from Specific Source

```sql
SELECT title, start_date_time, venue, price_from, price_to
FROM events
WHERE scraped_from = 'theloft.at'
ORDER BY start_date_time DESC;
```

### Find Duplicate Events

```sql
SELECT dedup_hash, title, COUNT(*) as count
FROM events
WHERE dedup_hash IS NOT NULL
GROUP BY dedup_hash
HAVING COUNT(*) > 1;
```

### Latest Scraping Activity

```sql
SELECT scraped_from, COUNT(*) as events, MAX(created_at) as last_scraped
FROM events
WHERE created_at > NOW() - INTERVAL '7 days'
GROUP BY scraped_from
ORDER BY last_scraped DESC;
```

### Update Selectors for a Website

```sql
UPDATE scrape_rules
SET 
    title_selector = '.new-title-class',
    date_selector = '.new-date-class',
    updated_at = CURRENT_TIMESTAMP
WHERE site_name = 'daswerk.org';
```

### Enable/Disable Website

```sql
-- Disable
UPDATE scrape_rules SET enabled = false WHERE site_name = 'theloft.at';

-- Re-enable
UPDATE scrape_rules SET enabled = true WHERE site_name = 'theloft.at';
```

### Clear All Events from a Source

```sql
DELETE FROM events WHERE scraped_from = 'theloft.at';
```

### View Raw HTML

```sql
SELECT title, raw_source_html 
FROM events 
WHERE scraped_from = 'theloft.at' 
LIMIT 1;

-- Output will show the <div class="event">...</div> or similar snippet
```

---

## Accessing the Database

### Connect via Docker

```bash
# Open psql terminal
docker exec -it eventfinder_db psql -U eventfinder -d eventfinder

# Common commands in psql
\dt                  -- List tables
\d events            -- Describe events table
\d scrape_rules      -- Describe scrape_rules table
SELECT * FROM scrape_rules;  -- View all rules
```

### GUI Tools

- **DBeaver Community** (free) - Full IDE for database management
- **pgAdmin** - Web interface for PostgreSQL
- **VS Code Extension**: "SQLTools" + "PostgreSQL Driver"

---

## Flyway Migrations

### How Migrations Work

1. **First startup**: Flyway checks `db/migration/` folder
2. **Runs in order**: V1, V2, V3 (numbered prefix)
3. **Tracks history**: `flyway_schema_history` table records applied migrations
4. **Idempotent**: Safe to restart (won't re-run completed migrations)

### Viewing Applied Migrations

```bash
# Check Docker logs
docker logs eventfinder_backend | grep -i "flyway\|migration"

# Or query database
SELECT version, description, success, installed_on 
FROM flyway_schema_history 
ORDER BY installed_on;
```

### Adding New Migration

To add a website or modify schema:

1. Create file: `backend/src/main/resources/db/migration/V4__YourDescription.sql`
2. Write SQL in uppercase (convention)
3. Restart containers: `docker-compose restart backend`
4. Flyway automatically detects and runs new migration

---

## Backup & Recovery

### Backup Database

```bash
# Export to SQL file
docker exec eventfinder_db pg_dump -U eventfinder eventfinder > backup.sql

# Or backup with Docker volume
docker run --rm -v eventfinder_db_data:/data -v $(pwd):/backup \
  postgres tar czf /backup/db-backup.tar.gz -C /data .
```

### Restore Database

```bash
# From SQL file
docker exec -i eventfinder_db psql -U eventfinder eventfinder < backup.sql

# Or restore volume
docker run --rm -v eventfinder_db_data:/data -v $(pwd):/backup \
  postgres tar xzf /backup/db-backup.tar.gz -C /data
```

---

## Performance Tuning

### Recommended Indexes (add as needed)

```sql
-- If filtering events by date range frequently
CREATE INDEX idx_event_start_date ON events(start_date_time DESC);

-- If searching by location
CREATE INDEX idx_event_location ON events(location);

-- If title search needed
CREATE INDEX idx_event_title ON events USING GIN(to_tsvector('german', title));

-- If high volume of rules queries
CREATE INDEX idx_scrape_rules_enabled ON scrape_rules(enabled);
```

### Monitoring

```sql
-- Table sizes
SELECT 
    schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Row counts
SELECT relname, n_live_tup as rows FROM pg_stat_user_tables;
```

---

## Troubleshooting

### Migration Failed

```bash
# Check logs
docker logs eventfinder_backend | tail -50

# If migration is stuck, you may need to manually fix and reset
# (Only do this in development!)
docker exec eventfinder_db psql -U eventfinder eventfinder -c \
  "DELETE FROM flyway_schema_history WHERE version = 3;"
```

### Can't Connect to Database

```bash
# Check if container is running
docker ps | grep db

# View db logs
docker logs eventfinder_db

# Restart
docker-compose restart db
```

### Slow Queries

```bash
# Enable query logging
docker exec eventfinder_db psql -U eventfinder eventfinder -c \
  "ALTER SYSTEM SET log_min_duration_statement = 1000;"

# Then check logs
docker logs eventfinder_db | grep "duration:"
```
