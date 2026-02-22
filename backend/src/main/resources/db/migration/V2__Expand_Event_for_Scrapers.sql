-- Add new scraper-friendly fields to events table
-- Migration V2: Multi-source scraper support with HTML storage, pricing ranges, deduplication, etc.

-- Add pricing fields (support price ranges)
ALTER TABLE events ADD COLUMN IF NOT EXISTS price_from DECIMAL(10, 2);
ALTER TABLE events ADD COLUMN IF NOT EXISTS price_to DECIMAL(10, 2);
ALTER TABLE events ADD COLUMN IF NOT EXISTS price_note VARCHAR(500);

-- Add venue field (physical location or room names)
ALTER TABLE events ADD COLUMN IF NOT EXISTS venue VARCHAR(500);

-- Add recurrence tracking
ALTER TABLE events ADD COLUMN IF NOT EXISTS is_recurring BOOLEAN DEFAULT FALSE;
ALTER TABLE events ADD COLUMN IF NOT EXISTS recurring_pattern VARCHAR(100);

-- Add scraper metadata
ALTER TABLE events ADD COLUMN IF NOT EXISTS scraped_from VARCHAR(100);
ALTER TABLE events ADD COLUMN IF NOT EXISTS parser_version VARCHAR(50);
ALTER TABLE events ADD COLUMN IF NOT EXISTS dedup_hash VARCHAR(64);

-- Add raw HTML storage for offline re-parsing
ALTER TABLE events ADD COLUMN IF NOT EXISTS raw_source_html VARCHAR(50000);

-- Add timestamps for created/updated tracking
ALTER TABLE events ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE events ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Create event_tags table for many-to-many tag relationship
CREATE TABLE IF NOT EXISTS event_tags (
    event_id BIGINT NOT NULL,
    tag VARCHAR(100) NOT NULL,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, tag)
);

-- Create index on dedup_hash for fast duplicate detection
CREATE INDEX IF NOT EXISTS idx_event_dedup_hash ON events(dedup_hash);

-- Create index on scraped_from for filtering by source
CREATE INDEX IF NOT EXISTS idx_event_scraped_from ON events(scraped_from);

-- Create index on created_at for sorting/filtering
CREATE INDEX IF NOT EXISTS idx_event_created_at ON events(created_at);

-- Create index on is_recurring for querying recurring events
CREATE INDEX IF NOT EXISTS idx_event_is_recurring ON events(is_recurring);
