-- EventFinder Database Schema - V1 (Consolidated)
-- Complete initial schema + seed data for clean rule-based scraping system
-- Single migration to avoid Flyway checksum validation issues

-- Create events table with all necessary fields for modern scraping
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP,
    location VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    category VARCHAR(255),
    source_url VARCHAR(255),
    image_url VARCHAR(255),
    organizer VARCHAR(255),
    price VARCHAR(255),
    venue VARCHAR(500),
    status VARCHAR(255),
    
    -- Pricing fields (support price ranges)
    price_from DECIMAL(10, 2),
    price_to DECIMAL(10, 2),
    price_note VARCHAR(500),
    
    -- Recurrence tracking
    is_recurring BOOLEAN DEFAULT FALSE,
    recurring_pattern VARCHAR(100),
    
    -- Scraper metadata
    scraped_from VARCHAR(100),
    parser_version VARCHAR(50),
    dedup_hash VARCHAR(64),
    raw_source_html VARCHAR(50000),
    
    -- Timestamps
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    last_updated TIMESTAMP,
    
    CONSTRAINT events_start_date_time_check CHECK (start_date_time IS NOT NULL)
);

-- Create places table for cached location data
CREATE TABLE places (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    last_updated TIMESTAMP
);

-- Create venues table for event venues
CREATE TABLE venues (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    location VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    category VARCHAR(100),
    capacity INTEGER,
    website VARCHAR(255),
    phone VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create scrape_rules table for configurable rule-based scraping
CREATE TABLE scrape_rules (
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

-- Create event_tags table for many-to-many tag relationship
CREATE TABLE event_tags (
    event_id BIGINT NOT NULL,
    tag VARCHAR(100) NOT NULL,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, tag)
);

-- Create indexes for performance optimization
CREATE INDEX idx_event_dedup_hash ON events(dedup_hash);
CREATE INDEX idx_event_scraped_from ON events(scraped_from);
CREATE INDEX idx_event_created_at ON events(created_at);
CREATE INDEX idx_event_is_recurring ON events(is_recurring);
CREATE INDEX idx_events_start_date ON events(start_date_time);
CREATE INDEX idx_events_category ON events(category);
CREATE INDEX idx_events_location ON events(location);
CREATE INDEX idx_venues_name ON venues(name);
CREATE INDEX idx_places_coordinates ON places(latitude, longitude);
CREATE INDEX idx_scrape_rules_site_name ON scrape_rules(site_name);
CREATE INDEX idx_scrape_rules_enabled ON scrape_rules(enabled);

-- The Loft (theloft.at) - WordPress site with REGEX extraction from link text
INSERT INTO scrape_rules (
    site_name, base_url, event_list_selector, event_item_selector,
    title_selector, date_selector, price_selector, venue_selector, link_selector,
    date_format, date_locale, extraction_mode, event_text_pattern, category, organizer,
    enabled, ai_enabled, requires_detail_page_fetch, notes, created_at
) VALUES (
    'theloft.at', 'https://www.theloft.at/programm/',
    'a[href*="/programm/"]', 'a[href*="/programm/"]',
    'a', 'a', 'a', 'a', 'a',
    'E. d.M.yyyy HH:mm', 'de-DE', 'REGEX',
    '([A-Za-z]{2}\.\s+\d{1,2}\.\d{1,2}\.\d{4}\s+\d{1,2}:\d{2}),?\s*(?:Eintritt:\s*)?(.*?)\s+([A-ZÄÖÜ][^\s]+(?:\s+[A-ZÄÖÜ][^\s]+)*?)\s+(Oben|Unten|Wohnzimmer|Keller|Dachboden)',
    'Event', 'The Loft',
    true, true, false,
    'The Loft - WordPress site. Uses REST API /wp-json/wp/v2/posts if available. Falls back to REGEX extraction from link text. Format: "Day. DD.MM.YYYY HH:mm, Eintritt: Price TITLE Venue"',
    CURRENT_TIMESTAMP
);

-- Grelle Forelle (grelleforelle.com) - Electronic music venue
INSERT INTO scrape_rules (
    site_name, base_url, event_list_selector, event_item_selector,
    title_selector, date_selector, price_selector, venue_selector, link_selector, image_selector,
    date_format, date_locale, extraction_mode, category, organizer,
    enabled, ai_enabled, requires_detail_page_fetch, notes, created_at
) VALUES (
    'grelleforelle.com', 'https://grelleforelle.com/programm',
    '.events-list, .event-grid', '.et_pb_portfolio_item',
    '.et_pb_portfolio_title, h2, h3', '.event-date, .date, time',
    '.event-price, .price', '.event-location, .venue',
    'a', '.event-image img, img',
    'd/M', 'de-DE', 'CSS_SELECTOR',
    'Music', 'Grelle Forelle',
    true, true, false,
    'Electronic music venue in Vienna. Uses .et_pb_portfolio_item for event containers (18 events extracted).',
    CURRENT_TIMESTAMP
);

-- Das Werk (daswerk.org) - Cultural events venue
INSERT INTO scrape_rules (
    site_name, base_url, event_list_selector, event_item_selector,
    title_selector, date_selector, price_selector, venue_selector, link_selector, image_selector, description_selector,
    date_format, date_locale, extraction_mode, category, organizer,
    enabled, ai_enabled, requires_detail_page_fetch, notes, created_at
) VALUES (
    'daswerk.org', 'https://www.daswerk.org/programm/',
    '.program-list, .events', '.events--preview-item',
    '.preview-item--headline', '.preview-item--information li:first-child',
    '.price, .eintritt', '.location, .ort',
    '.preview-item--link', 'img',
    '.description, .beschreibung, p',
    'd. MMMM yyyy HH:mm', 'de-DE', 'CSS_SELECTOR',
    'Culture', 'Das Werk',
    true, true, false,
    'Das Werk - Cultural center with event items in .events--preview-item. Date in first li of .preview-item--information. Supports Austrian German (Feber for February, März for March, etc.). Successfully extracting 7 events.',
    CURRENT_TIMESTAMP
);

-- Save Date (savedate.io/@prst) - Event aggregator platform
INSERT INTO scrape_rules (
    site_name, base_url, event_list_selector, event_item_selector,
    title_selector, date_selector, price_selector, venue_selector, link_selector, image_selector, description_selector,
    date_format, date_locale, extraction_mode, category, organizer,
    enabled, ai_enabled, requires_detail_page_fetch, notes, created_at
) VALUES (
    'savedate.io/@prst', 'https://savedate.io/@prst',
    '[data-testid="event-list"], .events, main', '[data-testid="event-card"], .event-card, article',
    '[data-testid="event-title"], .event-title, h2, h3',
    '[data-testid="event-date"], .event-date, time, .date',
    '[data-testid="event-price"], .event-price, .price',
    '[data-testid="event-venue"], .event-venue, .venue',
    'a[href*="/event/"]', '[data-testid="event-image"], .event-image img, img',
    '[data-testid="event-description"], .event-description, p',
    'yyyy-MM-dd''T''HH:mm:ss', 'en-US', 'CSS_SELECTOR',
    'Event', 'PRST',
    true, true, false,
    'SaveDate.io platform. Uses modern data attributes and ISO date formats.',
    CURRENT_TIMESTAMP
);

-- Falter.at - Vienna culture & events magazine
INSERT INTO scrape_rules (
    site_name, base_url, event_list_selector, event_item_selector,
    title_selector, date_selector, venue_selector, link_selector, image_selector,
    description_selector,
    date_format, date_locale, extraction_mode, category, organizer,
    enabled, ai_enabled, requires_detail_page_fetch, notes, created_at
) VALUES (
    'falter.at', 'https://www.falter.at/events',
    'body', 'a[data-section="item"]',
    'h1', 'time',
    '.lining-nums', 'this',
    'figure img',
    'p.pb-1\\.5',
    'EEEE, HH:mm', 'de-AT', 'CSS_SELECTOR',
    'Culture', 'Falter',
    true, false, false,
    'Falter.at - Vienna culture & events. Uses semantic HTML with data attributes. Date format includes German day names (Montag, Dienstag, etc.) parsed by parseGermanDayName() method.',
    CURRENT_TIMESTAMP
);
