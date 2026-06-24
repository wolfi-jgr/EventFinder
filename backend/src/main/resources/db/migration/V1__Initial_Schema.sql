-- EventFinder Database Schema - V1 (Consolidated)
-- Complete initial schema + seed data + category normalization for clean rule-based scraping system
-- Single migration to avoid Flyway checksum validation issues

-- Create events table with all necessary fields for modern scraping
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    start_date DATE NOT NULL,
    start_time TIME,
    end_date DATE,
    end_time TIME,
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
    
    CONSTRAINT events_start_date_check CHECK (start_date IS NOT NULL)
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

-- Create users table for admin access
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ADMIN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed admin user (change password in production)
INSERT INTO users (username, password_hash, role)
VALUES ('admin', '$2b$12$f6vEPq8dpS4S0e10bN4.W.ApqlGabNIL6UgLynGlBHOEhldEkfs5y', 'ADMIN');

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
CREATE INDEX idx_events_start_date ON events(start_date);
CREATE INDEX idx_events_start_time ON events(start_time);
CREATE INDEX idx_events_category ON events(category);
CREATE INDEX idx_events_location ON events(location);
CREATE INDEX idx_venues_name ON venues(name);
CREATE INDEX idx_places_coordinates ON places(latitude, longitude);
CREATE INDEX idx_scrape_rules_site_name ON scrape_rules(site_name);
CREATE INDEX idx_scrape_rules_enabled ON scrape_rules(enabled);


-- --------------------------------------------------------------------------
-- Category normalization to EventCategory enum names
-- (Merged from former V2__Normalize_Categories.sql)
-- --------------------------------------------------------------------------

-- CONCERT
UPDATE events SET category = 'CONCERT'
WHERE lower(category) IN ('music', 'konzert', 'concert', 'live musik', 'live music',
                                                    'musik', 'gig', 'band', 'dj', 'festival');

-- PARTY
UPDATE events SET category = 'PARTY'
WHERE lower(category) IN ('party', 'night', 'clubnight', 'rave', 'dance');

-- THEATER
UPDATE events SET category = 'THEATER'
WHERE lower(category) IN ('theater', 'theatre', 'oper', 'opera', 'schauspiel',
                                                    'kabarett', 'cabaret', 'bühne', 'comedy');

-- EXHIBITION
UPDATE events SET category = 'EXHIBITION'
WHERE lower(category) IN ('ausstellung', 'exhibition', 'galerie', 'gallery',
                                                    'kunst', 'vernissage', 'art', 'installation');

-- SPORTS
UPDATE events SET category = 'SPORTS'
WHERE lower(category) IN ('sport', 'sports', 'fitness', 'yoga', 'turnier',
                                                    'tournament', 'match', 'marathon');

-- FOOD
UPDATE events SET category = 'FOOD'
WHERE lower(category) IN ('food', 'essen', 'kulinarisch', 'markt', 'market',
                                                    'brunch', 'tasting', 'wine', 'wein', 'cooking');

-- BUSINESS
UPDATE events SET category = 'BUSINESS'
WHERE lower(category) IN ('conference', 'konferenz', 'meetup', 'networking',
                                                    'talk', 'seminar', 'summit', 'lecture', 'vortrag');

-- WORKSHOP
UPDATE events SET category = 'WORKSHOP'
WHERE lower(category) IN ('workshop', 'kurs', 'course', 'class', 'kreativ', 'craft');

-- FAMILY
UPDATE events SET category = 'FAMILY'
WHERE lower(category) IN ('family', 'familien', 'kinder', 'kids', 'jugend', 'youth');

-- Anything remaining that is non-null and not already an enum name → OTHER
UPDATE events SET category = 'OTHER'
WHERE category IS NOT NULL
    AND category NOT IN ('CONCERT','PARTY','THEATER','EXHIBITION','SPORTS',
                                             'FOOD','BUSINESS','WORKSHOP','FAMILY','EVENT','OTHER');

-- Rows with no category → EVENT (generic fallback)
UPDATE events SET category = 'EVENT'
WHERE category IS NULL OR trim(category) = '';
