-- Create scrape_rules table and insert initial configurations for target websites
-- These are starting configurations that may need adjustment after testing

-- Create the scrape_rules table
CREATE TABLE IF NOT EXISTS scrape_rules (
    id BIGSERIAL PRIMARY KEY,
    site_name VARCHAR(255) NOT NULL UNIQUE,
    base_url VARCHAR(255) NOT NULL,
    event_list_selector VARCHAR(1000),
    event_item_selector VARCHAR(1000),
    title_selector VARCHAR(500),
    date_selector VARCHAR(500),
    price_selector VARCHAR(500),
    venue_selector VARCHAR(500),
    link_selector VARCHAR(500),
    image_selector VARCHAR(500),
    description_selector VARCHAR(500),
    date_format VARCHAR(100),
    date_locale VARCHAR(100),
    extraction_mode VARCHAR(50),
    event_text_pattern VARCHAR(1000),
    category VARCHAR(100),
    organizer VARCHAR(100),
    requires_detail_page_fetch BOOLEAN DEFAULT FALSE,
    detail_page_selector VARCHAR(500),
    ai_enabled BOOLEAN DEFAULT FALSE,
    ai_prompt VARCHAR(500),
    rate_limit_ms INTEGER,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    notes VARCHAR(2000)
);

-- 1. The Loft (theloft.at)
-- Link text format: "Di. 3.3.2026 19:00, Eintritt: € freie Spende SOLIDRAGITY Wohnzimmer"
INSERT INTO scrape_rules (
    site_name, base_url, 
    event_list_selector, event_item_selector,
    title_selector, date_selector, price_selector, venue_selector, link_selector,
    date_format, date_locale,
    extraction_mode, category, organizer,
    enabled, ai_enabled, requires_detail_page_fetch,
    notes, created_at
) VALUES (
    'theloft.at',
    'https://www.theloft.at/programm/',
    'a[href*="/programm/"]',  -- Links in program section
    'a[href*="/programm/"]',  -- Each link is an event
    'a',                       -- Title from link text (will need parsing)
    'a',                       -- Date from link text
    'a',                       -- Price from link text
    'a',                       -- Venue from link text
    'a',                       -- Link itself
    'E. d.M.yyyy HH:mm',      -- "Di. 3.3.2026 19:00" - flexible format
    'de-DE',
    'REGEX',                   -- Use regex extraction for text parsing
    'Event',
    'The Loft',
    true,
    true,                      -- Enable AI fallback
    false,
    'The Loft parses event info from link text. Format: "Day. DD.MM.YYYY HH:mm, Eintritt: Price TITLE Venue"',
    CURRENT_TIMESTAMP
);

-- Set regex pattern for The Loft (captures date, time, price, title, venue)
UPDATE scrape_rules 
SET event_text_pattern = '([A-Za-z]{2}\.\s+\d{1,2}\.\d{1,2}\.\d{4}\s+\d{1,2}:\d{2}),?\s*(?:Eintritt:\s*)?(.*?)\s+([A-ZÄÖÜ][^\s]+(?:\s+[A-ZÄÖÜ][^\s]+)*?)\s+(Oben|Unten|Wohnzimmer|Keller|Dachboden)'
WHERE site_name = 'theloft.at';

-- 2. Grelle Forelle (grelleforelle.com)
-- Typical electronic music venue with event cards
INSERT INTO scrape_rules (
    site_name, base_url,
    event_list_selector, event_item_selector,
    title_selector, date_selector, price_selector, venue_selector, link_selector, image_selector,
    date_format, date_locale,
    extraction_mode, category, organizer,
    enabled, ai_enabled, requires_detail_page_fetch,
    notes, created_at
) VALUES (
    'grelleforelle.com',
    'https://grelleforelle.com/events',
    '.events-list, .event-grid',
    '.event-item, .event-card, article',
    '.event-title, h2, h3',
    '.event-date, .date, time',
    '.event-price, .price',
    '.event-location, .venue',
    'a',
    '.event-image img, img',
    'd/M',                     -- Typical short format "DD/MM"
    'de-DE',
    'CSS_SELECTOR',
    'Music',
    'Grelle Forelle',
    true,
    true,
    false,
    'Electronic music venue. May need detail page fetch for full description.',
    CURRENT_TIMESTAMP
);

-- 3. Das Werk (daswerk.org)
-- Cultural venue with German month names
INSERT INTO scrape_rules (
    site_name, base_url,
    event_list_selector, event_item_selector,
    title_selector, date_selector, price_selector, venue_selector, link_selector, image_selector, description_selector,
    date_format, date_locale,
    extraction_mode, category, organizer,
    enabled, ai_enabled, requires_detail_page_fetch,
    notes, created_at
) VALUES (
    'daswerk.org',
    'https://daswerk.org/programm',
    '.program-list, .events',
    '.program-item, .event',
    '.title, h2, h3',
    '.date, .datum, time',
    '.price, .eintritt',
    '.location, .ort',
    'a[href*="/event/"], a[href*="/programm/"]',
    'img',
    '.description, .beschreibung, p',
    'd. MMMM yyyy HH:mm',      -- "3. März 2026 19:00" or "Feber/März"
    'de-DE',
    'CSS_SELECTOR',
    'Culture',
    'Das Werk',
    true,
    true,
    false,
    'Cultural center with German date formats. Uses "Feber" for February.',
    CURRENT_TIMESTAMP
);

-- 4. Save Date / PRST (savedate.io/@prst)
-- Modern event platform
INSERT INTO scrape_rules (
    site_name, base_url,
    event_list_selector, event_item_selector,
    title_selector, date_selector, price_selector, venue_selector, link_selector, image_selector, description_selector,
    date_format, date_locale,
    extraction_mode, category, organizer,
    enabled, ai_enabled, requires_detail_page_fetch,
    notes, created_at
) VALUES (
    'savedate.io/@prst',
    'https://savedate.io/@prst',
    '[data-testid="event-list"], .events, main',
    '[data-testid="event-card"], .event-card, article',
    '[data-testid="event-title"], .event-title, h2, h3',
    '[data-testid="event-date"], .event-date, time, .date',
    '[data-testid="event-price"], .event-price, .price',
    '[data-testid="event-venue"], .event-venue, .venue',
    'a[href*="/event/"]',
    '[data-testid="event-image"], .event-image img, img',
    '[data-testid="event-description"], .event-description, p',
    'yyyy-MM-dd''T''HH:mm:ss',  -- ISO format common in modern platforms
    'en-US',
    'CSS_SELECTOR',
    'Event',
    'PRST',
    true,
    true,
    false,
    'SaveDate.io platform. May use modern data attributes. Likely ISO date formats.',
    CURRENT_TIMESTAMP
);

-- Create index for faster lookups (IF NOT EXISTS for idempotency)
CREATE INDEX IF NOT EXISTS idx_scrape_rules_site_name ON scrape_rules(site_name);
CREATE INDEX IF NOT EXISTS idx_scrape_rules_enabled ON scrape_rules(enabled);
