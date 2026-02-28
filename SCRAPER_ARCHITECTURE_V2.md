# EventFinder Scraping Architecture v2 - Modular Design

## Overview

The new architecture breaks up the monolithic `GenericScraper` into a modular, strategy-based system where each scraper type handles one extraction mode.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                   ScrapeOrchestrationService                     │
│  (Orchestrates scraping workflow, caching, deduplication)        │
└──────────────────────┬──────────────────────────────────────────┘
                       │ uses
                       ▼
        ┌──────────────────────────────┐
        │     ScraperFactory           │
        │  (Config-driven selection)   │
        └───────────┬──────────────────┘
                    │ creates
       ┌────────────┼────────────┬──────────────┐
       │            │            │              │
       ▼            ▼            ▼              ▼
  Hybrid      WordPress      Regex         CssSelector
  Scraper     Scraper       Scraper        Scraper
  (tries      (REST         (pattern       (selectors)
   WP→HTML)   /wp-json)     matching)
       │            │            │              │
       └────────────┼────────────┴──────────────┘
                    │ all implement
                    ▼
            ┌─────────────────┐
            │  EventScraper   │
            │   Interface     │
            └────────────┬────┘
                         │
                extends BaseWebScraper
                         │
                         ▼
            ┌──────────────────────────┐
            │  BaseWebScraper          │
            │  (Rate limit, retries)   │
            └──────────────────────────┘

Shared Utilities:
├─ PriceExtractor (parse pricing)
├─ HtmlExtractor (safe element access)
├─ DateParser (parse dates in multiple formats)
└─ EventMapper (ScrapedEvent → Event)
```

## Scraper Types

### 1. **HybridScraper**
- **Purpose**: Auto-detect and use the best method for each site
- **Logic**: Try WordPress API → Fall back to rule's extraction mode (CSS/Regex)
- **Used for**: Most sites (flexible, automatic)
- **Extraction Mode**: `HYBRID` or `AUTO`

### 2. **WordPressScraper**
- **Purpose**: Extract from WordPress REST API (/wp-json/wp/v2/posts)
- **Always tries**: WordPress detection first
- **Falls back to**: HTML parsing if API fails
- **Extraction Mode**: `WORDPRESS` (explicit)

### 3. **CssSelectorScraper**
- **Purpose**: CSS selector-based extraction for static HTML
- **Config**: Uses `eventItemSelector`, `titleSelector`, `dateSelector`, etc.
- **Extraction Mode**: `CSS_SELECTOR` or default

### 4. **RegexScraper**
- **Purpose**: Extract from text patterns using regex
- **Config**: Uses `eventTextPattern` with named groups
- **Extraction Mode**: `REGEX`

## Configuration Examples

### Hybrid (Auto-detect)
```yaml
- site-name: example.com
  base-url: https://example.com/events
  extraction-mode: HYBRID  # Try WordPress first, fall back to CSS
  event-list-selector: .events
  event-item-selector: .event
  title-selector: h2
  date-selector: .date
```

### WordPress Explicit
```yaml
- site-name: theloft.at
  base-url: https://www.theloft.at/programm/
  extraction-mode: WORDPRESS  # Direct API only
```

### CSS Selector
```yaml
- site-name: grelleforelle.com
  base-url: https://grelleforelle.com/programm
  extraction-mode: CSS_SELECTOR
  event-item-selector: .et_pb_portfolio_item
  title-selector: h2
  date-selector: time
```

### Regex
```yaml
- site-name: theloft.at
  base-url: https://www.theloft.at/programm/
  extraction-mode: REGEX
  event-text-pattern: '(\d{2}\.\d{2}\.\d{4})\s+(.+?)\s+€(\d+)'
```

## Migration Path

### Phase 1: Foundation (Current Status)
✅ Create `EventScraper` interface
✅ Create `ScraperFactory` 
✅ Create shared utilities (PriceExtractor, HtmlExtractor)
✅ Create `HtmlBasedScraper` base class

### Phase 2: Implement Individual Scrapers
- [ ] Create `CssSelectorScraper` (extract CSS logic from GenericScraper)
- [ ] Create `RegexScraper` (extract regex logic from GenericScraper)
- [ ] Create `WordPressScraper` (extract WordPress logic from GenericScraper)
- [ ] Create `HybridScraper` (orchestrate fallback logic)
- [ ] Extract common utilities (DateParser, EventMapper)

### Phase 3: Refactor Orchestration
- [ ] Update `ScrapeOrchestrationService` to use `ScraperFactory`
- [ ] Replace direct `GenericScraper` calls with factory-selected scrapers
- [ ] Remove config-based rules INSERT from SQL (already done via startup sync)

### Phase 4: Cleanup
- [ ] Remove `GenericScraper` monolith (deprecated)
- [ ] Remove old HTML-based tests using GenericScraper directly
- [ ] Document new architecture in team wiki

## Benefits

1. **Single Responsibility**: Each scraper handles one extraction mode
2. **Testability**: Can test each scraper independently
3. **Extensibility**: Adding new site types → new scraper, not changing GenericScraper
4. **Config-Driven**: Rules determine which scraper is used (no code changes)
5. **Clear Interfaces**: `EventScraper` contract is explicit
6. **Shared Logic**: Common extraction utilities reduce duplication
7. **Hybrid Support**: Automatic fallback without manual site configuration

## File Structure

```
backend/src/main/java/com/example/eventfinder/scraper/
├── EventScraper.java                    (interface)
├── ScraperFactory.java                  (config-driven selection)
├── BaseWebScraper.java                  (rate limiting, retries)
├── impl/
│   ├── HtmlBasedScraper.java            (base for HTML scrapers)
│   ├── CssSelectorScraper.java          (CSS extraction)
│   ├── RegexScraper.java                (regex extraction)
│   ├── WordPressScraper.java            (WordPress API)
│   ├── HybridScraper.java               (fallback orchestration)
│   └── GenericScraper.java              (DEPRECATED - will be removed)
├── util/
│   ├── PriceExtractor.java              (price parsing)
│   ├── HtmlExtractor.java               (safe element access)
│   ├── DateParser.java                  (date parsing)
│   └── EventMapper.java                 (ScrapedEvent → Event)
└── ScraperConfig.java
```

## Next Steps

1. Implement individual scrapers (CssSelectorScraper, RegexScraper, WordPressScraper, HybridScraper)
2. Extract common utilities used by all scrapers
3. Update ScrapeOrchestrationService to use factory
4. Run integration tests to ensure all 5 sites still work
5. Remove GenericScraper and old INSERT statements from SQL
