# Scraper Consolidation: Practical Action Plan

## Summary

You have two scraping systems:
- **Legacy:** falter.at uses `SourceUrl` + `GenericEventScraper` (heuristic scraping)
- **New:** 4 sites use `ScrapeRule` + `GenericScraper` (rule-based scraping)

**Goal:** Consolidate to one system (rule-based) for easier maintenance.

---

## Current Status

### Sites Using New Rule-Based System ✅
- theloft.at
- grelleforelle.com
- daswerk.org
- savedate.io/@prst

**Evidence:** HTML cached in `backend/scraper-data/` (dated 2026-02-22)

### Sites Using Legacy System ⚠️
- falter.at

**Evidence:** No HTML cache, defined in `SourceUrlService.java`, uses ScraperManager

---

## Quick Start: Migrate falter.at

### Step 1: Capture falter.at HTML (5 min)

Run this to save falter.at HTML for analysis:

```bash
cd backend
curl -H "User-Agent: Mozilla/5.0" https://www.falter.at/events -o scraper-data/falter.at_manual.html
```

Or use the existing scraping system to capture it:
```bash
# Via API (if backend is running)
curl -X POST http://localhost:8080/api/scrape/source/1
```

### Step 2: Analyze HTML Structure (15 min)

Open the saved HTML and identify:
- What wraps each event? (article, div, li?)
- Where's the title? (h2, h3, span?)
- Where's the date? (time element, span?)
- Where's the venue? (class names?)
- Where's the link? (a href?)

**Example analysis:**
```html
<!-- If you see this structure: -->
<article class="event-card">
    <h2 class="event-title">Concert Name</h2>
    <time datetime="2026-03-01">1. März 2026</time>
    <span class="location">Arena Vienna</span>
    <a href="/event/123">Details</a>
</article>

<!-- Your selectors would be: -->
event_item_selector: 'article.event-card'
title_selector: 'h2.event-title'
date_selector: 'time'
venue_selector: 'span.location'
link_selector: 'a'
```

### Step 3: Create ScrapeRule (10 min)

Connect to your database and run:

```sql
INSERT INTO scrape_rules (
    site_name, 
    base_url,
    event_item_selector,      -- UPDATE with actual selectors
    title_selector,           -- UPDATE with actual selectors
    date_selector,            -- UPDATE with actual selectors
    price_selector,           -- UPDATE with actual selectors
    venue_selector,           -- UPDATE with actual selectors
    link_selector,            -- UPDATE with actual selectors
    image_selector,           -- UPDATE with actual selectors
    date_format,              -- UPDATE with actual format
    date_locale,
    extraction_mode,
    category,
    enabled,
    created_at,
    updated_at,
    notes
) VALUES (
    'falter.at',
    'https://www.falter.at/events',
    'TODO: article.event-item',     -- ← Replace with actual selector
    'TODO: h2.event-title',          -- ← Replace with actual selector
    'TODO: time.event-date',         -- ← Replace with actual selector
    'TODO: .event-price',            -- ← Replace with actual selector
    'TODO: .event-location',         -- ← Replace with actual selector
    'TODO: a.event-link',            -- ← Replace with actual selector
    'TODO: img.event-image',         -- ← Replace with actual selector
    'd. MMMM yyyy',                  -- ← Adjust based on actual format
    'de-AT',
    'CSS_SELECTOR',
    'All',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'Migrated from legacy SourceUrl system on 2026-02-23'
);
```

### Step 4: Test the Rule (15 min)

**Option A: Via API**
```bash
curl -X POST http://localhost:8080/api/scrape/site/falter.at
```

**Option B: Via Frontend**
Visit the scraping admin panel and trigger scrape for falter.at

**Check:**
- How many events were extracted?
- Are titles correct?
- Are dates correct?
- Are venues correct?

### Step 5: Refine Selectors (15-30 min)

If extraction isn't working perfectly:

```sql
-- Update selectors without code changes!
UPDATE scrape_rules 
SET 
    title_selector = '.actual-title-class',
    date_selector = '.actual-date-class'
WHERE site_name = 'falter.at';
```

Then test again (repeat until working).

### Step 6: Disable Legacy Source (5 min)

Once the new rule works:

```sql
-- Disable or delete the old SourceUrl
UPDATE source_urls 
SET is_enabled = false 
WHERE domain = 'falter.at';

-- Or delete it completely
DELETE FROM source_urls WHERE domain = 'falter.at';
```

### Step 7: Verify Migration (10 min)

Run full scraping and check:
- falter.at events appear in database
- No errors in logs
- Event count is reasonable

---

## Enhancements to Consider

### Add Location Blacklisting to GenericScraper

**File:** `backend/src/main/java/com/example/eventfinder/scraper/impl/GenericScraper.java`

Add this constant and use it when extracting venues:

```java
private static final Set<String> LOCATION_BLACKLIST = Set.of(
    "location", "ort", "venue", "veranstaltungsort", "address", 
    "wien", "vienna", "theater", "kino", "museum"
);

private boolean isValidVenueName(String location) {
    if (location == null || location.length() < 3) return false;
    String lower = location.toLowerCase().trim();
    return !LOCATION_BLACKLIST.contains(lower);
}
```

### Add Relative Date Parsing

**File:** `backend/src/main/java/com/example/eventfinder/scraper/impl/GenericScraper.java`

In the `parseDateTime` method, add before other parsing:

```java
// Handle German relative dates
String lower = cleaned.toLowerCase();
if (lower.contains("heute")) {
    return LocalDate.now().atTime(19, 0);
}
if (lower.contains("morgen")) {
    return LocalDate.now().plusDays(1).atTime(19, 0);
}
if (lower.contains("übermorgen") || lower.contains("uebermorgen")) {
    return LocalDate.now().plusDays(2).atTime(19, 0);
}
```

---

## Fallback Plan: If Rule-Based Doesn't Work

If falter.at HTML is too complex or unpredictable:

### Option 1: Use REGEX Mode

Instead of CSS selectors, use regex extraction:

```sql
UPDATE scrape_rules 
SET 
    extraction_mode = 'REGEX',
    event_text_pattern = '([^<]+)\\s+(\\d{1,2}\\.\\s*\\w+\\s*\\d{4})'
WHERE site_name = 'falter.at';
```

### Option 2: Keep falter.at on Legacy System

If it's working fine and falter.at is the only site using the legacy system:
- Keep both systems short term
- Plan to migrate when time allows
- Document why it's still separate

### Option 3: AI-Assisted Extraction (Future)

Enable AI fallback mode:
```sql
UPDATE scrape_rules 
SET 
    ai_enabled = true,
    ai_prompt = 'Extract events from this HTML: title, date, venue, price'
WHERE site_name = 'falter.at';
```

---

## Timeline Estimate

| Task | Time | Status |
|------|------|--------|
| Capture HTML | 5 min | ⏳ Ready |
| Analyze structure | 15 min | ⏳ Ready |
| Create rule | 10 min | ⏳ Ready |
| Test rule | 15 min | ⏳ Ready |
| Refine selectors | 15-30 min | ⏳ Ready |
| Disable legacy | 5 min | ⏳ Ready |
| Verify | 10 min | ⏳ Ready |
| **Total** | **1-1.5 hours** | |

---

## Verification Checklist

After migration, verify:

- [ ] falter.at rule exists in `scrape_rules` table
- [ ] Rule is enabled (`enabled = true`)
- [ ] Scraping produces events
- [ ] Event fields are populated (title, date, venue)
- [ ] No errors in application logs
- [ ] HTML is cached in `scraper-data/falter.at_YYYYMMDD.html`
- [ ] Old SourceUrl is disabled or deleted
- [ ] Full scrape includes falter.at events

---

## Support Commands

### Check Current Sources
```sql
-- Legacy system
SELECT id, name, url, scraper_type, is_enabled 
FROM source_urls;

-- New system  
SELECT id, site_name, base_url, extraction_mode, enabled
FROM scrape_rules;
```

### Check Recent Events
```sql
SELECT title, start_date_time, location, category, created_at
FROM events
WHERE source_url LIKE '%falter%'
ORDER BY created_at DESC
LIMIT 20;
```

### View Logs
```bash
# If running via Docker
docker-compose logs -f backend

# If running locally
tail -f backend/logs/application.log
```

---

## Need Help?

If you get stuck:

1. **Check the saved HTML**: Does it match your selectors?
2. **Check the logs**: What errors do you see?
3. **Simplify selectors**: Start broad, then narrow
4. **Test incrementally**: One field at a time

**I can help with:**
- Analyzing the HTML structure
- Writing the selectors
- Debugging extraction issues
- Implementing enhancements

Just share:
- The saved HTML or a snippet
- Any error messages
- What fields aren't extracting correctly

---

## Long-Term Goals

Once falter.at is migrated:

1. **Deprecate Legacy System**
   - Mark `GenericEventScraper` as `@Deprecated`
   - Remove `ScraperManager` usage
   - Drop `source_urls` table

2. **Enhance Rule-Based System**
   - Add web UI for managing rules
   - Implement AI fallback mode
   - Add rule testing tools

3. **Monitor & Optimize**
   - Track extraction success rates
   - Auto-disable broken rules
   - Alert on parsing failures

---

*Created: February 23, 2026*
*For: EventFinder Scraper Consolidation*
