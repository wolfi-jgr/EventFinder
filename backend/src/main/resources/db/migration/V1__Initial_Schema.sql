-- Initial schema for EventFinder application
-- Migration V1: Create base tables

-- Create events table
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
    price VARCHAR(255), -- Legacy field
    venue VARCHAR(255),
    status VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    last_updated TIMESTAMP,
    CONSTRAINT events_start_date_time_check CHECK (start_date_time IS NOT NULL)
);

-- Create places table
CREATE TABLE places (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    source_url VARCHAR(255),
    last_updated TIMESTAMP
);

-- Create source_urls table
CREATE TABLE source_urls (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    category VARCHAR(255),
    domain VARCHAR(255),
    scraper_type VARCHAR(255),
    is_enabled BOOLEAN DEFAULT true,
    is_system BOOLEAN DEFAULT false,
    last_scraped_at TIMESTAMP,
    last_error_at TIMESTAMP,
    last_error_message VARCHAR(1000),
    failure_count INTEGER DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create scrape_rules table
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
    requires_detail_page_fetch BOOLEAN,
    detail_page_selector VARCHAR(500),
    ai_enabled BOOLEAN,
    ai_prompt VARCHAR(500),
    rate_limit_ms INTEGER,
    enabled BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    notes VARCHAR(2000)
);

-- Create indexes for better query performance
CREATE INDEX idx_events_start_date_time ON events(start_date_time);
CREATE INDEX idx_events_category ON events(category);
CREATE INDEX idx_events_location ON events(location);
CREATE INDEX idx_places_name ON places(name);
CREATE INDEX idx_source_urls_domain ON source_urls(domain);
CREATE INDEX idx_source_urls_is_enabled ON source_urls(is_enabled);
CREATE INDEX idx_scrape_rules_site_name ON scrape_rules(site_name);
CREATE INDEX idx_scrape_rules_enabled ON scrape_rules(enabled);
