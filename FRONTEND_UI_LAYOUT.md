# Frontend UI Architecture & Layout Guide

## Overview

EventFinder has two frontend implementations:
- **Web Frontend**: React with Vite (responsive web app)
- **Mobile Frontend**: Expo (React Native for iOS/Android)

Both share a centralized configuration (`shared/frontendConfig.js`) for themes, API endpoints, and app constants.

---

## Current Architecture

### App Structure (App.jsx)

```
┌─────────────────────────────────────────────┐
│              App Component                   │
├─────────────────────────────────────────────┤
│  ┌─────────────┬────────────┬──────────────┐│
│  │  "Events"   │  "Places"  │   "Admin"    ││ ← Tab Navigation
│  └─────────────┴────────────┴──────────────┘│
│                                              │
│  ┌──────────────────────────────────────────┤
│  │                                           │
│  │  Content Area (varies by active tab)     │
│  │                                           │
│  │  - EventList (for "Events")               │
│  │  - PlacesList (for "Places")              │
│  │  - ScrapingPanel (for "Admin")            │
│  │                                           │
│  └──────────────────────────────────────────┘
│                                              │
│  ┌──────────────────────────────────────────┤
│  │ Search/Filter Controls                   │
│  │ - Location selector                      │
│  │ - Date range picker (optional)           │
│  │ - Category filter (optional)              │
│  └──────────────────────────────────────────┘
└─────────────────────────────────────────────┘
```

### States

```javascript
const [coords, setCoords] = useState(DEFAULT_COORDS);        // User location
const [places, setPlaces] = useState([]);                    // Venue list
const [events, setEvents] = useState([]);                    // All events
const [loading, setLoading] = useState(false);               // Loading state
const [error, setError] = useState("");                      // Error or success message
const [activeTab, setActiveTab] = useState("events");        // Current view
```

---

## REQUESTED: Horizontal Slider (Events per Location)

### Current Layout Issue

Currently, events may be displayed in a **vertical scrolling list** regardless of which location they're from. This makes it hard to compare events at different venues.

### Proposed Solution: Location-Based Horizontal Carousel

```
┌─────────────────────────────────────────────────────┐
│ Location: The Loft                                   │
├─────────────────────────────────────────────────────┤
│  ◄   [Event Card 1]  [Event Card 2]  [Event Card 3]  [Event Card 4]   ►
│     (3/3 - March 3  (4/3 - March 4  (5/3 - March 5 (6/3 - March 6    
│      19:00)          19:00)          19:00)          - Coming Soon)
│      
│ Location: Grelle Forelle
├─────────────────────────────────────────────────────┤
│  ◄   [Event Card 1]  [Event Card 2]                  ►
│     (22/3 - "Sick..." (23/3 - "Bass...")             
│      20:00)           21:00)
│
│ Location: Das Werk
├─────────────────────────────────────────────────────┤
│  ◄   [Event Card 1]  [Event Card 2]  [Event Card 3]  ►
│     ("March 3")      ("March 10")     ("March 17")
```

### Implementation Strategy

#### Step 1: Group Events by Location

```javascript
// In App.jsx
const groupEventsByLocation = () => {
  const grouped = {};
  
  events.forEach(event => {
    const locationKey = event.location || event.venue || "Unknown";
    if (!grouped[locationKey]) {
      grouped[locationKey] = [];
    }
    grouped[locationKey].push(event);
  });
  
  // Sort events within each location by date
  Object.keys(grouped).forEach(location => {
    grouped[location].sort((a, b) => 
      new Date(a.startDateTime) - new Date(b.startDateTime)
    );
  });
  
  return grouped;
};

const eventsByLocation = groupEventsByLocation();
```

#### Step 2: Create LocationSlider Component

**File**: `frontend/src/components/LocationSlider.jsx`

```jsx
import { useState, useRef } from "react";
import "./LocationSlider.css";

export default function LocationSlider({ location, events }) {
  const [scrollPosition, setScrollPosition] = useState(0);
  const scrollContainerRef = useRef(null);

  const scroll = (direction) => {
    const container = scrollContainerRef.current;
    const scrollAmount = 320; // Width of card + gap
    
    if (direction === "left") {
      container.scrollBy({ left: -scrollAmount, behavior: "smooth" });
    } else {
      container.scrollBy({ left: scrollAmount, behavior: "smooth" });
    }
  };

  const handleScroll = () => {
    if (scrollContainerRef.current) {
      setScrollPosition(scrollContainerRef.current.scrollLeft);
    }
  };

  const canScrollLeft = scrollPosition > 0;
  const canScrollRight = scrollContainerRef.current &&
    scrollPosition < scrollContainerRef.current.scrollWidth - scrollContainerRef.current.clientWidth;

  return (
    <div className="location-slider">
      <h2 className="location-name">{location}</h2>
      
      <div className="slider-container">
        {canScrollLeft && (
          <button className="scroll-button scroll-left" onClick={() => scroll("left")}>
            ◄
          </button>
        )}
        
        <div
          className="scroll-area"
          ref={scrollContainerRef}
          onScroll={handleScroll}
        >
          {events.map((event) => (
            <EventCard key={event.id} event={event} />
          ))}
        </div>
        
        {canScrollRight && (
          <button className="scroll-button scroll-right" onClick={() => scroll("right")}>
            ►
          </button>
        )}
      </div>
    </div>
  );
}
```

**File**: `frontend/src/components/LocationSlider.css`

```css
.location-slider {
  margin-bottom: 3rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 1.5rem;
  background: var(--card-bg);
}

.location-name {
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0 0 1rem 0;
  color: var(--text-primary);
}

.slider-container {
  position: relative;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.scroll-area {
  display: flex;
  gap: 1rem;
  overflow-x: auto;
  flex: 1;
  scroll-behavior: smooth;
  scrollbar-width: thin;
  scrollbar-color: var(--scrollbar-thumb) transparent;
  padding: 0.5rem 0;
}

.scroll-area::-webkit-scrollbar {
  height: 6px;
}

.scroll-area::-webkit-scrollbar-track {
  background: transparent;
}

.scroll-area::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb);
  border-radius: 3px;
}

.scroll-button {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border: 1px solid var(--border-color);
  background: var(--button-bg);
  color: var(--text-primary);
  font-size: 1.2rem;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.scroll-button:hover {
  background: var(--button-hover-bg);
  transform: scale(1.1);
}

.scroll-button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
```

#### Step 3: Update App.jsx to Use Location Sliders

```jsx
return (
  <div className="app">
    {/* Tab Navigation */}
    <div className="tab-navigation">
      {/* ... existing tabs ... */}
    </div>

    {/* Events Tab with Location Sliders */}
    {activeTab === "events" && (
      <div className="events-container">
        <div className="controls">
          {/* Filters/search */}
        </div>

        <div className="location-sliders">
          {Object.entries(eventsByLocation).map(([location, locationEvents]) => (
            <LocationSlider
              key={location}
              location={location}
              events={locationEvents}
            />
          ))}
        </div>
      </div>
    )}
  </div>
);
```

---

## REQUESTED: Vertical Pagination (Different Locations)

### Current Layout Issue

With many locations (5-10 venues), showing all location sliders causes **page height to grow excessively**, requiring lots of vertical scrolling.

### Proposed Solution: Paginated Venue List

```
┌─────────────────────────────────────────────┐
│ Showing Venues: 1-3 of 10                   │
│ [Previous]                          [Next]  │
├─────────────────────────────────────────────┤
│                                             │
│ Location: The Loft [4 events]               │
│  ◄  [Card 1] [Card 2] [Card 3] [Card 4]  ► │
│                                             │
│ Location: Grelle Forelle [2 events]         │
│  ◄  [Card 1] [Card 2]                     ► │
│                                             │
│ Location: Das Werk [5 events]               │
│  ◄  [Card 1] [Card 2] [Card 3]...         ► │
│                                             │
├─────────────────────────────────────────────┤
│ [Previous]                          [Next]  │
└─────────────────────────────────────────────┘
```

### Implementation Strategy

#### Step 1: Paginate Locations

```javascript
// In App.jsx
const LOCATIONS_PER_PAGE = 3; // Show 3 venues per page

const [currentPage, setCurrentPage] = useState(1);

const getLocationsPaginated = () => {
  const locationList = Object.entries(eventsByLocation);
  const totalPages = Math.ceil(locationList.length / LOCATIONS_PER_PAGE);
  const pageIndex = Math.max(0, currentPage - 1);
  const start = pageIndex * LOCATIONS_PER_PAGE;
  const end = start + LOCATIONS_PER_PAGE;
  
  return {
    data: locationList.slice(start, end),
    currentPage,
    totalPages,
    totalLocations: locationList.length,
  };
};

const handleNextPage = () => {
  const { totalPages } = getLocationsPaginated();
  if (currentPage < totalPages) {
    setCurrentPage(currentPage + 1);
  }
};

const handlePreviousPage = () => {
  if (currentPage > 1) {
    setCurrentPage(currentPage - 1);
  }
};
```

#### Step 2: Create VenuePagination Component

**File**: `frontend/src/components/VenuePagination.jsx`

```jsx
import LocationSlider from "./LocationSlider";
import "./VenuePagination.css";

export default function VenuePagination({
  locations,
  currentPage,
  totalPages,
  totalLocations,
  onNextPage,
  onPreviousPage,
}) {
  const itemsPerPage = locations.length; // 3 in this example
  const startIndex = (currentPage - 1) * itemsPerPage + 1;
  const endIndex = Math.min(currentPage * itemsPerPage, totalLocations);

  return (
    <div className="venue-pagination">
      {/* Header with pagination info */}
      <div className="pagination-header">
        <span className="pagination-info">
          Showing venues {startIndex}-{endIndex} of {totalLocations}
        </span>
      </div>

      {/* Location sliders for current page */}
      <div className="locations-container">
        {locations.map(([location, events]) => (
          <LocationSlider
            key={location}
            location={location}
            events={events}
          />
        ))}
      </div>

      {/* Pagination controls */}
      <div className="pagination-controls">
        <button
          className="pagination-button"
          onClick={onPreviousPage}
          disabled={currentPage === 1}
        >
          ← Previous
        </button>

        <div className="page-indicator">
          Page {currentPage} of {totalPages}
        </div>

        <button
          className="pagination-button"
          onClick={onNextPage}
          disabled={currentPage === totalPages}
        >
          Next →
        </button>
      </div>
    </div>
  );
}
```

**File**: `frontend/src/components/VenuePagination.css`

```css
.venue-pagination {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.pagination-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: var(--secondary-bg);
  border-radius: 4px;
}

.pagination-info {
  font-weight: 500;
  color: var(--text-primary);
}

.locations-container {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.pagination-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 2rem;
  padding: 1.5rem;
  background: var(--secondary-bg);
  border-radius: 4px;
}

.pagination-button {
  padding: 0.75rem 1.5rem;
  border: 1px solid var(--border-color);
  background: var(--button-bg);
  color: var(--text-primary);
  font-weight: 500;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.pagination-button:hover:not(:disabled) {
  background: var(--button-hover-bg);
  transform: translateY(-2px);
}

.pagination-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-indicator {
  font-weight: 600;
  color: var(--text-primary);
  min-width: 150px;
  text-align: center;
}
```

#### Step 3: Update App.jsx

```jsx
// Within the Events tab section:
{activeTab === "events" && (
  <div className="events-container">
    <div className="controls">
      {/* Filters/search */}
    </div>

    <VenuePagination
      locations={getLocationsPaginated().data}
      currentPage={currentPage}
      totalPages={getLocationsPaginated().totalPages}
      totalLocations={getLocationsPaginated().totalLocations}
      onNextPage={handleNextPage}
      onPreviousPage={handlePreviousPage}
    />
  </div>
)}
```

---

## Website List Component

### Display Scraped Websites

Create a new component to show which websites are being scraped and their status:

**File**: `frontend/src/components/ScrapedWebsites.jsx`

```jsx
import { useState, useEffect } from "react";
import { API_BASE } from "../config";
import "./ScrapedWebsites.css";

export default function ScrapedWebsites() {
  const [websites, setWebsites] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    fetchWebsiteStatus();
  }, []);

  const fetchWebsiteStatus = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE}/api/scraping/rules/status`);
      if (!response.ok) {
        throw new Error(`Failed to fetch website status: ${response.status}`);
      }
      const data = await response.json();
      setWebsites(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="scraping-websites loading">Loading websites...</div>;
  }

  return (
    <div className="scraping-websites">
      <h2>Scraped Websites</h2>
      
      {error && <div className="error-message">{error}</div>}

      {websites.length === 0 ? (
        <p>No websites configured yet.</p>
      ) : (
        <div className="websites-grid">
          {websites.map((site) => (
            <div
              key={site.siteName}
              className={`website-card ${site.enabled ? "enabled" : "disabled"}`}
            >
              <div className="website-header">
                <h3>{site.siteName}</h3>
                <span className={`status-badge ${site.enabled ? "active" : "inactive"}`}>
                  {site.enabled ? "Active" : "Inactive"}
                </span>
              </div>

              <div className="website-info">
                <p>
                  <strong>Mode:</strong> {site.extractionMode}
                </p>
                <p>
                  <strong>Events:</strong> {site.eventCount || 0}
                </p>
                <p>
                  <strong>Cached:</strong>{" "}
                  {site.hasCachedHtml ? "✓ Yes" : "No"}
                </p>
                {site.aiEnabled && (
                  <p>
                    <strong>AI Fallback:</strong> Enabled
                  </p>
                )}
              </div>

              <div className="website-actions">
                <a
                  href={site.baseUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="button button-link"
                >
                  Visit Website
                </a>
              </div>
            </div>
          ))}
        </div>
      )}

      <button className="refresh-button" onClick={fetchWebsiteStatus}>
        Refresh Status
      </button>
    </div>
  );
}
```

**File**: `frontend/src/components/ScrapedWebsites.css`

```css
.scraping-websites {
  padding: 2rem;
  background: var(--card-bg);
  border-radius: 8px;
  margin: 1rem 0;
}

.scraping-websites h2 {
  margin-top: 0;
  color: var(--text-primary);
}

.websites-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.5rem;
  margin: 2rem 0;
}

.website-card {
  border: 2px solid var(--border-color);
  border-radius: 8px;
  padding: 1.5rem;
  background: var(--card-bg);
  transition: all 0.3s;
}

.website-card.enabled {
  border-color: #4caf50;
  background: rgba(76, 175, 80, 0.05);
}

.website-card.disabled {
  border-color: #f44336;
  background: rgba(244, 67, 54, 0.05);
  opacity: 0.6;
}

.website-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.website-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  gap: 1rem;
}

.website-header h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 1.1rem;
  word-break: break-word;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.status-badge.active {
  background: #c8e6c9;
  color: #2e7d32;
}

.status-badge.inactive {
  background: #ffcdd2;
  color: #c62828;
}

.website-info {
  margin: 1rem 0;
}

.website-info p {
  margin: 0.75rem 0;
  font-size: 0.95rem;
  color: var(--text-secondary);
}

.website-info strong {
  color: var(--text-primary);
}

.website-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
}

.button-link {
  flex: 1;
  padding: 0.5rem 1rem;
  text-align: center;
  background: var(--button-bg);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  text-decoration: none;
  font-size: 0.9rem;
  transition: all 0.2s;
  cursor: pointer;
}

.button-link:hover {
  background: var(--button-hover-bg);
}

.refresh-button {
  padding: 0.75rem 1.5rem;
  background: var(--button-bg);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
  margin-top: 1rem;
  transition: all 0.2s;
}

.refresh-button:hover {
  background: var(--button-hover-bg);
}

.loading {
  text-align: center;
  padding: 2rem;
  color: var(--text-secondary);
}

.error-message {
  padding: 1rem;
  background: #ffebee;
  color: #c62828;
  border-radius: 4px;
  margin-bottom: 1rem;
}
```

---

## Final Component Integration

### Updated App.jsx Structure

```jsx
import VenuePagination from "./components/VenuePagination";
import ScrapedWebsites from "./components/ScrapedWebsites";

export default function App() {
  // ... existing state ...
  const [currentPage, setCurrentPage] = useState(1);

  // ... existing methods ...

  const eventsByLocation = groupEventsByLocation();
  const paginatedLocations = getLocationsPaginated();

  return (
    <div className="app">
      <header className="app-header">
        <h1>🎭 EventFinder</h1>
        <p>Discover events in Vienna</p>
      </header>

      <div className="tab-navigation">
        <button
          className={`tab ${activeTab === "events" ? "active" : ""}`}
          onClick={() => {
            setActiveTab("events");
            setCurrentPage(1); // Reset to first page
          }}
        >
          Events
        </button>
        <button
          className={`tab ${activeTab === "places" ? "active" : ""}`}
          onClick={() => setActiveTab("places")}
        >
          Venues
        </button>
        <button
          className={`tab ${activeTab === "admin" ? "active" : ""}`}
          onClick={() => setActiveTab("admin")}
        >
          Admin
        </button>
      </div>

      {activeTab === "events" && (
        <VenuePagination
          locations={paginatedLocations.data}
          currentPage={paginatedLocations.currentPage}
          totalPages={paginatedLocations.totalPages}
          totalLocations={paginatedLocations.totalLocations}
          onNextPage={handleNextPage}
          onPreviousPage={handlePreviousPage}
        />
      )}

      {activeTab === "places" && (
        <div className="places-container">
          {/* Existing places list */}
        </div>
      )}

      {activeTab === "admin" && (
        <div className="admin-container">
          <ScrapingPanel />
          <ScrapedWebsites />
        </div>
      )}
    </div>
  );
}
```

---

## Shared Theme Configuration

The `shared/frontendConfig.js` centralizes theme/color variables:

```javascript
export const FRONTEND_THEME = {
  colors: {
    primary: "#2196F3",
    secondary: "#FF9800",
    success: "#4CAF50",
    error: "#F44336",
    warning: "#FFC107",
    background: "#FFFFFF",
    surface: "#F5F5F5",
    text: "#212121",
    textSecondary: "#757575",
    border: "#E0E0E0",
  },
  spacing: {
    xs: "0.25rem",
    sm: "0.5rem",
    md: "1rem",
    lg: "1.5rem",
    xl: "2rem",
  },
};
```

Apply in CSS:

```css
:root {
  --primary-color: #2196F3;
  --card-bg: #FFFFFF;
  --text-primary: #212121;
  --border-color: #E0E0E0;
  --button-bg: #F5F5F5;
  --button-hover-bg: #E0E0E0;
  --scrollbar-thumb: #BDBDBD;
}
```

---

## Mobile Responsive Design

For Expo (mobile), adapt component structure:

**File**: `mobile-expo/src/screens/EventsScreen.js`

```jsx
import { ScrollView, FlatList, View, Text } from "react-native";
import LocationSlider from "../components/LocationSlider";

export default function EventsScreen({ locationsData }) {
  const renderLocation = ({ item: [location, events] }) => (
    <LocationSlider location={location} events={events} />
  );

  return (
    <ScrollView>
      <FlatList
        data={locationsData}
        renderItem={renderLocation}
        keyExtractor={([location]) => location}
        scrollEnabled={false}
      />
    </ScrollView>
  );
}
```

---

## Summary

**Horizontal Slider**: Events grouped by venue in scrollable cards
**Vertical Pagination**: Show 3 venues per page, paginate through all venues
**Website List**: Show which sites are being scraped and their status
**Responsive**: Works on web (Vite) and mobile (Expo)

All styling integrated with `FRONTEND_THEME` from `shared/frontendConfig.js`.
