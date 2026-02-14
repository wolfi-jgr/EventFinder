# Web Scraping System Documentation

## Overview

The EventFinder web scraping framework provides a robust, safe, and extensible system for extracting event data from websites without getting blocked.

## Key Features

✅ **Rate Limiting** - Automatic delays between requests (configurable)
✅ **User Agent Rotation** - Rotates through multiple user agents
✅ **Retry Logic** - Exponential backoff for failed requests
✅ **Concurrent Scraping** - Multiple sources scraped in parallel
✅ **Custom Sources** - Users can add their own URLs
✅ **Error Tracking** - Auto-disable sources after repeated failures
✅ **Extensible** - Easy to add new scraper implementations

## Architecture

### Core Components

1. **ScraperConfig** - Configuration for scraping behavior
2. **BaseWebScraper** - Abstract base class with rate limiting
3. **ScraperManager** - Coordinates all scrapers
4. **SourceUrl** - Entity for user-defined scraping sources
5. **GenericEventScraper** - Fallback scraper for common HTML structures

## Configuration

Edit `application.yml`:

```yaml
scraper:
  enabled: true                        # Enable/disable scraping
  min-delay-between-requests: 2000     # Min delay (ms)
  max-delay-between-requests: 5000     # Max delay (ms)
  connection-timeout: 10000            # Connection timeout (ms)
  max-retries: 3                       # Retry attempts
  max-concurrent-scrapers: 3           # Parallel scrapers
```

## API Endpoints

### Source URL Management

```bash
# Get all sources
GET /api/sources

# Get enabled sources only
GET /api/sources/enabled

# Get sources by category
GET /api/sources/category/{category}

# Add a new source
POST /api/sources
Content-Type: application/json
{
  "name": "Eventbrite Vienna",
  "url": "https://www.eventbrite.at/d/austria--vienna/events/",
  "description": "Events from Eventbrite",
  "category": "All",
  "scraperType": "generic"
}

# Update a source
PUT /api/sources/{id}

# Delete a source
DELETE /api/sources/{id}

# Toggle enabled status
POST /api/sources/{id}/toggle

# Initialize default sources
POST /api/sources/initialize
```

### Scraping Operations

```bash
# Run scraping for all enabled sources
POST /api/scraping/run

# Scrape a specific source
POST /api/scraping/source/{sourceId}

# Get available scraper types
GET /api/scraping/scrapers
```

## Usage Examples

### 1. Initialize Default Sources

```bash
curl -X POST http://localhost:8080/api/sources/initialize
```

### 2. Add Your Own Source

```bash
curl -X POST http://localhost:8080/api/sources \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Event Source",
    "url": "https://example.com/events",
    "description": "Custom event source",
    "category": "Music",
    "scraperType": "generic",
    "isEnabled": true
  }'
```

### 3. Run Scraping

```bash
curl -X POST http://localhost:8080/api/scraping/run
```

Response:
```json
{
  "totalEvents": 15,
  "successCount": 2,
  "failureCount": 0,
  "errors": []
}
```

### 4. Get All Sources

```bash
curl http://localhost:8080/api/sources
```

## Creating Custom Scrapers

To scrape a specific website, extend `BaseWebScraper`:

```java
@Component
public class EventbriteScraper extends BaseWebScraper {
    
    public EventbriteScraper(ScraperConfig config) {
        super(config);
    }
    
    @Override
    public String getScraperName() {
        return "Eventbrite Scraper";
    }
    
    @Override
    public String getTargetDomain() {
        return "eventbrite.at";
    }
    
    @Override
    public List<Event> scrapeEvents(String url) throws Exception {
        List<Event> events = new ArrayList<>();
        
        // Fetch page with automatic rate limiting
        Document doc = fetchDocument(url);
        
        // Select event elements (customize selectors)
        Elements eventElements = doc.select(".search-event-card");
        
        for (Element element : eventElements) {
            Event event = new Event();
            
            // Extract data using helper methods
            event.setTitle(safeText(element.selectFirst("h3")));
            event.setDescription(safeText(element.selectFirst(".summary")));
            event.setLocation(safeText(element.selectFirst(".location")));
            
            // Extract date
            String dateStr = safeAttr(element.selectFirst("time"), "datetime");
            if (dateStr != null) {
                event.setStartDateTime(LocalDateTime.parse(dateStr));
            }
            
            // Set metadata
            setEventMetadata(event);
            events.add(event);
        }
        
        return events;
    }
}
```

Then register it in `ScraperInitializer.java`:

```java
scraperManager.registerScraper("eventbrite", eventbriteScraper);
```

## Safety Features

### 1. Rate Limiting
- Random delays between requests (2-5 seconds default)
- Per-domain tracking to avoid hammering same site
- Configurable min/max delays

### 2. User Agent Rotation
- Rotates through realistic browser user agents
- Makes requests look like real browsers

### 3. Retry Logic
- Exponential backoff for failed requests
- Configurable retry attempts (default: 3)

### 4. Error Handling
- Graceful failure handling
- Tracks consecutive failures
- Auto-disables sources after 5 failures

### 5. Concurrent Control
- Limits parallel scrapers (default: 3)
- Prevents resource exhaustion

## Best Practices

1. **Start with Low Volume**
   - Test with 1-2 sources first
   - Increase gradually

2. **Respect Robots.txt**
   - Check if scraping is allowed
   - Add User-Agent header

3. **Monitor Error Logs**
   - Watch for 403/429 responses
   - Increase delays if needed

4. **Use Specific Scrapers**
   - Generic scraper works but is less accurate
   - Write custom scrapers for important sources

5. **Schedule Scraping**
   - Don't scrape continuously
   - Use scheduled jobs (e.g., daily)

## Troubleshooting

### Problem: Getting Blocked (403/429 errors)
**Solution:** Increase delays in `application.yml`

```yaml
scraper:
  min-delay-between-requests: 5000
  max-delay-between-requests: 10000
```

### Problem: No Events Found
**Solution:** 
- Check URL is correct
- Inspect HTML structure of target site
- Write custom scraper with correct selectors

### Problem: Timeouts
**Solution:** Increase timeout in config

```yaml
scraper:
  connection-timeout: 20000
```

### Problem: Source Auto-Disabled
**Solution:** Check `last_error_message` in database:

```sql
SELECT name, last_error_message, failure_count 
FROM source_urls 
WHERE is_enabled = false;
```

## Database Schema

The `source_urls` table stores:
- `url`, `name`, `description`
- `category`, `domain`, `scraper_type`
- `is_enabled`, `is_system`
- `last_scraped_at`, `last_error_at`
- `failure_count`, `last_error_message`
- `created_at`, `updated_at`

## Future Enhancements

- [ ] Scheduled scraping with cron expressions
- [ ] Webhook notifications on scraping completion
- [ ] Scraping history/analytics
- [ ] Rate limit per domain configuration
- [ ] Proxy support
- [ ] JavaScript rendering (Selenium/Playwright)
- [ ] Content deduplication across sources

## Support

For issues or questions, check:
- Backend logs: `docker logs eventfinder_backend`
- Database: Check `source_urls` table for error messages
- API: Use `/api/scraping/scrapers` to list available scrapers
