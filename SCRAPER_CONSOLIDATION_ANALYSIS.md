# Scraper System Consolidation Analysis

## Executive Summary

Your EventFinder project currently has **TWO PARALLEL scraping systems** that were developed at different times:

1. **Legacy System (SourceUrl + GenericEventScraper)** - Used by falter.at
2. **New System (ScrapeRule + GenericScraper)** - Rule-based configuration

Both work fine, but having two systems creates maintenance overhead and confusion. This document analyzes both approaches and recommends a unified strategy.

---

## Current State: Two Parallel Systems

### System 1: Legacy SourceUrl-based System

**Components:**
- `SourceUrl` entity (database table: `source_urls`)
- `GenericEventScraper` (819 lines)
- `ScraperManager` (orchestration)
- `SourceUrlService` (CRUD operations)

**How It Works:**
1. Sites are defined in `source_urls` table with minimal configuration
2. `GenericEventScraper` uses **heuristic scraping**:
   - Tries multiple common CSS selectors to find events
   - Uses blacklists and pattern matching
   - Extensive fallback logic for dates, locations, prices
   - Smart but brittle: works by guessing HTML structure

**Example: falter.at Configuration**
```java
// In SourceUrlService.java
createDefaultSource(
    "Falter Events Vienna",
    "https://www.falter.at/events",
    "General event calendar for Vienna",
    "All",
    "falter.at",
    "generic"  // Uses GenericEventScraper
);
```

**Strengths:**
- ✅ Quick setup for new sites (just add URL)
- ✅ Smart heuristics can handle various HTML structures
- ✅ Extensive date parsing logic (handles German months, relative dates, etc.)
- ✅ Robust fallback mechanisms
- ✅ Good location extraction with blacklisting

**Weaknesses:**
- ❌ Unpredictable - relies on guessing HTML structure
- ❌ Hard to tune for specific sites
- ❌ 819 lines of complex extraction logic
- ❌ No site-specific configuration
- ❌ Difficult to debug when it fails
- ❌ Performance: tries many selectors per site

---

### System 2: New Rule-based System

**Components:**
- `ScrapeRule` entity (database table: `scrape_rules`)
- `GenericScraper` (544 lines)
- `ScrapeOrchestrationService` (orchestration)
- `HtmlStorage` (caches HTML files)

**How It Works:**
1. Sites are configured in `scrape_rules` table with **specific CSS selectors**
2. `GenericScraper` uses **configured extraction**:
   - Exact CSS selectors for each field
   - Custom date formats and locales
   - Support for both CSS and REGEX extraction modes
   - Deterministic and predictable

**Example: theloft.at Configuration**
```sql
INSERT INTO scrape_rules (
    site_name, base_url, 
    event_item_selector,
    title_selector, date_selector, price_selector, venue_selector,
    date_format, date_locale,
    extraction_mode, category, organizer,
    enabled
) VALUES (
    'theloft.at',
    'https://www.theloft.at/programm/',
    'a[href*="/programm/"]',  -- Exact selector for events
    'a',                       -- Title from link text
    'a',                       -- Date from link text  
    'a',                       -- Price from link text
    'a',                       -- Venue from link text
    'E. d.M.yyyy HH:mm',      -- Exact date format
    'de-DE',
    'REGEX',                   -- Use regex extraction
    'Event',
    'The Loft',
    true
);
```

**Strengths:**
- ✅ Predictable - uses exact selectors
- ✅ Easy to tune per site via database
- ✅ Support for both CSS and REGEX modes
- ✅ Cleaner code (544 lines vs 819)
- ✅ Better performance (no trial-and-error)
- ✅ HTML caching via HtmlStorage
- ✅ Site-specific date formats and locales
- ✅ Transaction support for event deduplication

**Weaknesses:**
- ❌ Requires manual configuration per site
- ❌ Less date parsing flexibility than legacy system
- ❌ No built-in location blacklisting
- ❌ More initial setup effort

---

## Side-by-Side Comparison

| Feature | Legacy (GenericEventScraper) | New (GenericScraper + Rules) |
|---------|------------------------------|------------------------------|
| **Configuration Method** | Minimal (just URL) | Detailed (CSS selectors per field) |
| **Extraction Strategy** | Heuristic (tries many selectors) | Deterministic (exact selectors) |
| **Site-Specific Tuning** | Not possible | Full control via database |
| **Date Parsing** | Very flexible (multiple formats) | Configurable per site |
| **Code Complexity** | 819 lines (complex logic) | 544 lines (cleaner) |
| **Performance** | Slower (trial-and-error) | Faster (direct extraction) |
| **Predictability** | Low (guesses structure) | High (uses known structure) |
| **Debugging** | Hard (complex logic) | Easy (check selectors) |
| **HTML Caching** | No | Yes (HtmlStorage) |
| **Extraction Modes** | CSS only | CSS + REGEX |
| **Setup Time** | Fast | Slower |
| **Maintenance** | Difficult | Easy (update DB) |

---

## Lessons from Both Systems

### What GenericEventScraper Does Well:
1. **Flexible Date Parsing**
   - Handles German months (Januar, Feber, März)
   - Relative dates (heute, morgen, übermorgen)
   - Multiple date formats
   - Extracts time from combined strings
   
2. **Smart Location Extraction**
   - Blacklist of invalid venue names
   - Pattern matching for "im Theater", "in der Arena"
   - Venue name validation

3. **Robust Element Finding**
   - Multiple fallback selectors
   - Filters out navigation/footer elements
   - Schema.org event detection

### What GenericScraper Does Well:
1. **Structured Configuration**
   - Database-driven rules
   - Per-site customization
   - Easy to update without code changes

2. **Dual Extraction Modes**
   - CSS selectors for structured HTML
   - Regex for text-heavy pages (like theloft.at)

3. **Performance Optimizations**
   - HTML caching
   - Direct extraction (no guessing)

---

## Recommended Consolidation Strategy

### Option A: Migrate falter.at to Rule-Based System ✅ **RECOMMENDED**

**Approach:**
1. Keep the new rule-based system as the primary scraper
2. Migrate falter.at from `SourceUrl` to `ScrapeRule`
3. Enhance GenericScraper with best features from GenericEventScraper
4. Deprecate the legacy system

**Benefits:**
- ✅ One system to maintain
- ✅ Consistent scraping approach
- ✅ Better performance with HTML caching
- ✅ All sites configurable via database

**Migration Steps:**
1. Create a `ScrapeRule` for falter.at
2. Test the rule-based scraping
3. Enhance GenericScraper with missing features:
   - German month parsing ✅ (already has)
   - Location blacklisting
   - Relative date parsing
4. Switch falter.at to use ScrapeOrchestrationService
5. Deprecate ScraperManager and GenericEventScraper

---

### Option B: Keep Both Systems (NOT Recommended)

**When to consider:**
- If falter.at HTML is too unpredictable for rules
- If you need quick "add and forget" scraping
- If heuristic scraping yields better results

**Drawbacks:**
- ❌ Two codebases to maintain
- ❌ Confusion about which system to use
- ❌ Duplicate functionality

---

## Detailed Migration Plan

### Phase 1: Enhance GenericScraper (1-2 hours)

**Add missing capabilities from GenericEventScraper:**

1. **Location Blacklisting**
   ```java
   // Add to GenericScraper
   private static final Set<String> LOCATION_BLACKLIST = Set.of(
       "location", "ort", "venue", "wien", "vienna",
       "theater", "kino", "museum"
   );
   ```

2. **Relative Date Parsing**
   ```java
   // Add to parseDateTime()
   if (cleaned.contains("heute")) return LocalDate.now().atTime(19, 0);
   if (cleaned.contains("morgen")) return LocalDate.now().plusDays(1).atTime(19, 0);
   ```

3. **Better Venue Validation**
   - Check minimum length (3 chars)
   - Blacklist common non-venue words
   - Pattern matching for venue indicators

### Phase 2: Create falter.at Rule (1 hour)

**Inspect falter.at HTML structure:**
```bash
curl https://www.falter.at/events > falter_sample.html
```

**Create ScrapeRule in database:**
```sql
INSERT INTO scrape_rules (
    site_name, base_url,
    event_item_selector,
    title_selector,
    date_selector,
    price_selector,
    venue_selector,
    link_selector,
    image_selector,
    date_format,
    date_locale,
    extraction_mode,
    category,
    enabled,
    notes
) VALUES (
    'falter.at',
    'https://www.falter.at/events',
    '.event-item',              -- TODO: Inspect actual HTML
    '.event-title',             -- TODO: Inspect actual HTML
    '.event-date',              -- TODO: Inspect actual HTML
    '.event-price',             -- TODO: Inspect actual HTML
    '.event-location',          -- TODO: Inspect actual HTML
    'a.event-link',             -- TODO: Inspect actual HTML
    'img.event-image',          -- TODO: Inspect actual HTML
    'd.M.yyyy HH:mm',           -- Adjust based on actual format
    'de-AT',
    'CSS_SELECTOR',
    'All',
    true,
    'Migrated from legacy SourceUrl system'
);
```

### Phase 3: Test and Validate (2 hours)

1. **Test the new rule:**
   ```bash
   # Call the scrape orchestration endpoint
   curl -X POST http://localhost:8080/api/scrape/site/falter.at
   ```

2. **Compare results:**
   - Run both systems side-by-side
   - Compare event counts
   - Check data quality

3. **Iterate on selectors:**
   - Update `scrape_rules` table
   - Test again
   - No code changes needed!

### Phase 4: Deprecate Legacy System (1 hour)

1. **Remove falter.at from SourceUrl:**
   ```sql
   DELETE FROM source_urls WHERE domain = 'falter.at';
   ```

2. **Mark legacy classes as deprecated:**
   ```java
   @Deprecated(since = "2026-02", forRemoval = true)
   @Component
   public class GenericEventScraper extends BaseWebScraper {
   ```

3. **Update API endpoints:**
   - Point `/api/scrape/all` to ScrapeOrchestrationService
   - Keep `/api/sources/*` for legacy compatibility (if needed)

4. **Update documentation:**
   - Mark old system as deprecated
   - Document new rule-based approach

### Phase 5: Future Cleanup (Optional)

Once all sites use rules:
- Delete `GenericEventScraper.java`
- Delete `ScraperManager.java`
- Delete `SourceUrl` entity and table
- Remove legacy API endpoints

---

## Implementation Recommendations

### Short Term (Recommended Now)
1. ✅ Keep both systems temporarily
2. ✅ Enhance GenericScraper with best features from GenericEventScraper
3. ✅ Create and test falter.at rule
4. ✅ Switch falter.at to rule-based system

### Long Term (Future)
1. Migrate all SourceUrls to ScrapeRules (if any are added)
2. Remove legacy system completely
3. Add web UI for managing ScrapeRules
4. Consider AI extraction mode for complex sites

---

## Testing falter.at Migration

### Step 1: Inspect HTML Structure

Save a sample page and analyze:
```bash
cd backend/scraper-data
# Check if you already have a falter.at sample
ls -la | grep falter
```

### Step 2: Create Initial Rule

Start with broad selectors and narrow down:
```sql
-- Broad initial rule
event_item_selector = 'article, .event, div[class*="event"]'
```

### Step 3: Test Extraction

Use the scraping panel UI or API:
```bash
POST /api/scrape/site/falter.at
```

Check logs for:
- How many events were found
- Which fields are missing
- Parse errors

### Step 4: Refine Selectors

Update the database rule (no code changes!):
```sql
UPDATE scrape_rules 
SET title_selector = '.actual-title-class'
WHERE site_name = 'falter.at';
```

---

## Code Quality Observations

### GenericEventScraper (Legacy)
- **Pros:** Comprehensive, handles edge cases
- **Cons:** Complex, hard to modify, brittle
- **Lines:** 819 lines
- **Test Coverage:** Minimal (rely on heuristics)

### GenericScraper (New)
- **Pros:** Clean, modular, testable
- **Cons:** Less fallback logic (but more predictable)
- **Lines:** 544 lines  
- **Test Coverage:** Better (deterministic extraction)

**Code Quality Winner:** GenericScraper (new system)

---

## Performance Comparison

### GenericEventScraper (Legacy)
- Tries 17+ different selectors per page
- No HTML caching
- Pattern matching on every element

**Estimated time per site:** 5-10 seconds

### GenericScraper (New)
- Direct selector lookup
- HTML caching (reuses saved HTML)
- Minimal parsing logic

**Estimated time per site:** 1-3 seconds (with cache)

**Performance Winner:** GenericScraper (new system)

---

## Recommendation Summary

### Strategy: Migrate to Rule-Based System

**Do This:**
1. ✅ Enhance `GenericScraper` with missing features
2. ✅ Create `ScrapeRule` for falter.at
3. ✅ Test thoroughly
4. ✅ Migrate falter.at to new system
5. ✅ Mark legacy system as deprecated

**Don't Do:**
- ❌ Keep both systems long-term
- ❌ Add new sites to SourceUrl
- ❌ Enhance GenericEventScraper further

**Timeline:**
- Enhancement: 2 hours
- falter.at rule creation: 1 hour
- Testing: 2 hours
- Migration: 1 hour
- **Total: ~6 hours**

---

## Next Steps

1. **Decide:** Confirm you want to proceed with consolidation
2. **Inspect:** Get falter.at HTML structure (I can help with this)
3. **Enhance:** Add missing features to GenericScraper
4. **Create:** Build the falter.at ScrapeRule
5. **Test:** Validate the new rule works
6. **Migrate:** Switch to the new system
7. **Deprecate:** Mark legacy system for removal

---

## Questions to Consider

1. **Are there other sites using SourceUrl?** (Check: `SELECT * FROM source_urls`)
2. **Is falter.at's HTML predictable?** (Need to inspect structure)
3. **What's your priority?** (Quick migration vs. feature parity)
4. **Do you want AI extraction fallback?** (For unpredictable HTML)

---

## Conclusion

**The rule-based system (GenericScraper + ScrapeRule) is superior:**
- ✅ Better performance
- ✅ Easier maintenance
- ✅ More predictable
- ✅ Cleaner code
- ✅ Database-driven configuration

**Migrate falter.at to the new system and deprecate the legacy system.**

This gives you one unified scraping strategy that's maintainable, performant, and easy to extend.

---

*Analysis Date: February 23, 2026*
*Created by: GitHub Copilot*
