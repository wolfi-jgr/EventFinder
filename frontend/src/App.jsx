import { useEffect, useState } from "react";
import "./App.css";
import ScrapingPanel from "./ScrapingPanel";
import LocationSlider from "./components/LocationSlider";
import ScrapedWebsites from "./components/ScrapedWebsites";
import { API_BASE } from "./config";
import { applyFrontendTheme } from "./theme";
import { APP_CONFIG } from "@shared/frontendConfig";

// ── Category metadata matching the backend EventCategory enum ──────────────
export const CATEGORY_INFO = {
  CONCERT:    { label: "Konzert",       icon: "🎵" },
  PARTY:      { label: "Party",         icon: "🎉" },
  THEATER:    { label: "Theater",       icon: "🎭" },
  EXHIBITION: { label: "Ausstellung",   icon: "🖼️" },
  SPORTS:     { label: "Sport",         icon: "⚽" },
  FOOD:       { label: "Food",          icon: "🍕" },
  BUSINESS:   { label: "Business",      icon: "💼" },
  WORKSHOP:   { label: "Workshop",      icon: "🔧" },
  FAMILY:     { label: "Familie",       icon: "👨‍👩‍👧" },
  EVENT:      { label: "Veranstaltung", icon: "📅" },
  OTHER:      { label: "Sonstiges",     icon: "✨" },
};

const formatDate = (d) => d.toISOString().slice(0, 10);

const getEventDateKey = (event) => {
  if (event.startDate) return event.startDate;
  if (event.startDateTime) return String(event.startDateTime).slice(0, 10);
  return "";
};

const getEventTimestamp = (event) => {
  if (event.startDateTime) {
    const parsed = new Date(event.startDateTime).getTime();
    if (!Number.isNaN(parsed)) return parsed;
  }

  if (event.startDate) {
    const time = event.startTime || "00:00:00";
    const parsed = new Date(`${event.startDate}T${time}`).getTime();
    if (!Number.isNaN(parsed)) return parsed;
  }

  return 0;
};

const DEFAULT_COORDS = APP_CONFIG.defaultCoords;

export default function App() {
  const [coords, setCoords] = useState(DEFAULT_COORDS);
  const [places, setPlaces] = useState([]);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [loadingMessage, setLoadingMessage] = useState("");
  const [activeTab, setActiveTab] = useState("events"); // "places", "events", or "admin"
  const [searchText, setSearchText] = useState("");
  const [selectedSource, setSelectedSource] = useState("all");
  const [selectedLocation, setSelectedLocation] = useState("all");
  const [selectedCategory, setSelectedCategory] = useState("all");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [sortMode, setSortMode] = useState("date-asc");
  const [activeDatePreset, setActiveDatePreset] = useState(""); // "today"|"tomorrow"|"weekend"

  // ── Date-preset helpers ───────────────────────────────────────────────────
  const applyDatePreset = (preset) => {
    if (activeDatePreset === preset) {
      // second click toggles off
      setDateFrom("");
      setDateTo("");
      setActiveDatePreset("");
      return;
    }
    const now = new Date();
    if (preset === "today") {
      const d = formatDate(now);
      setDateFrom(d);
      setDateTo(d);
    } else if (preset === "tomorrow") {
      const t = new Date(now);
      t.setDate(now.getDate() + 1);
      const d = formatDate(t);
      setDateFrom(d);
      setDateTo(d);
    } else if (preset === "weekend") {
      const dow = now.getDay(); // 0 = Sun, 6 = Sat
      let from, to;
      if (dow === 6) {
        // today is Saturday
        from = new Date(now);
        to = new Date(now);
        to.setDate(now.getDate() + 1);
      } else if (dow === 0) {
        // today is Sunday
        from = new Date(now);
        to = new Date(now);
      } else {
        // weekday → next Sat + Sun
        const daysToSat = 6 - dow;
        from = new Date(now);
        from.setDate(now.getDate() + daysToSat);
        to = new Date(from);
        to.setDate(from.getDate() + 1);
      }
      setDateFrom(formatDate(from));
      setDateTo(formatDate(to));
    }
    setActiveDatePreset(preset);
  };

  // Clear active preset label when user types a date manually
  const handleDateFromChange = (val) => { setDateFrom(val); setActiveDatePreset(""); };
  const handleDateToChange   = (val) => { setDateTo(val);   setActiveDatePreset(""); };

  // Load data on initial mount (without scraping)
  useEffect(() => {
    applyFrontendTheme();
    loadDataFromDatabase();
  }, []);

  const loadDataFromDatabase = async () => {
    setLoading(true);
    setLoadingMessage("Loading data...");
    setError("");

    try {
      // Fetch places
      const placesUrl = `${API_BASE}/api/places?lat=${coords.lat}&lon=${coords.lon}`;
      const placesResponse = await fetch(placesUrl);
      if (!placesResponse.ok) {
        throw new Error(`Places request failed with ${placesResponse.status}`);
      }
      const placesData = await placesResponse.json();
      setPlaces(placesData);

      // Fetch all events (including scraped ones)
      const eventsUrl = `${API_BASE}/api/events`;
      const eventsResponse = await fetch(eventsUrl);
      if (!eventsResponse.ok) {
        throw new Error(`Events request failed with ${eventsResponse.status}`);
      }
      const eventsData = await eventsResponse.json();
      setEvents(eventsData);
    } catch (err) {
      setError(err.message || "Request failed");
    } finally {
      setLoading(false);
      setLoadingMessage("");
    }
  };

  const fetchData = async () => {
    setLoading(true);
    setLoadingMessage("Scraping events...");
    setError("");

    try {
      // Step 1: Run rule-based scraper for all configured websites
      const scrapingResponse = await fetch(`${API_BASE}/api/scraping/rules/run`, {
        method: "POST",
      });

      if (!scrapingResponse.ok) {
        throw new Error(`Scraping failed with ${scrapingResponse.status}`);
      }

      const scrapingResult = await scrapingResponse.json();
      console.log("Scraping completed:", scrapingResult);

      // Step 2: Load fresh data from database
      setLoadingMessage("Loading fresh data...");
      await loadDataFromDatabase();

      setLoadingMessage("");

      // Show detailed results
      const newEvents = scrapingResult.totalNew || 0;
      const duplicates = scrapingResult.totalDuplicates || 0;
      const sitesProcessed = scrapingResult.sitesProcessed || 0;

      if (newEvents > 0) {
        let message = `✓ Scraping complete! ${newEvents} new event${newEvents !== 1 ? "s" : ""} from ${sitesProcessed} website${sitesProcessed !== 1 ? "s" : ""}`;
        if (duplicates > 0) {
          message += `, ${duplicates} duplicate${duplicates !== 1 ? "s" : ""} skipped`;
        }
        setError(message);
      } else if (duplicates > 0) {
        setError(
          `✓ Scraping complete! ${duplicates} duplicate${duplicates !== 1 ? "s" : ""} found from ${sitesProcessed} website${sitesProcessed !== 1 ? "s" : ""} (no new events)`
        );
      } else {
        setError(
          `Scraping completed from ${sitesProcessed} website${sitesProcessed !== 1 ? "s" : ""}, but no events found.`
        );
      }
    } catch (err) {
      setError(err.message || "Scraping failed");
      setLoading(false);
      setLoadingMessage("");
    }
  };

  const normalized = (value) => (value || "").toLowerCase();
  const getSourceLabel = (event) => event.scrapedFrom || event.organizer || "Unknown Source";
  const getLocationLabel = (event) => event.location || event.venue || "Unknown Location";

  const filteredEvents = events
    .filter((event) => {
      const source = getSourceLabel(event);
      const location = getLocationLabel(event);
      const category = event.category || "Uncategorized";
      const matchesSearch =
        normalized(event.title).includes(normalized(searchText)) ||
        normalized(event.description).includes(normalized(searchText)) ||
        normalized(source).includes(normalized(searchText)) ||
        normalized(location).includes(normalized(searchText)) ||
        normalized(category).includes(normalized(searchText));
      const matchesSource = selectedSource === "all" || source === selectedSource;
      const matchesLocation = selectedLocation === "all" || location === selectedLocation;
      const matchesCategory = selectedCategory === "all" || category === selectedCategory;

      const eventDateKey = getEventDateKey(event);
      const fromOk = !dateFrom || (eventDateKey && eventDateKey >= dateFrom);
      const toOk = !dateTo || (eventDateKey && eventDateKey <= dateTo);

      return matchesSearch && matchesSource && matchesLocation && matchesCategory && fromOk && toOk;
    })
    .sort((a, b) => {
      if (sortMode === "title") {
        return (a.title || "").localeCompare(b.title || "");
      }
      const aDate = getEventTimestamp(a);
      const bDate = getEventTimestamp(b);
      return sortMode === "date-desc" ? bDate - aDate : aDate - bDate;
    });

  const uniqueSources = Array.from(new Set(events.map(getSourceLabel))).sort((a, b) =>
    a.localeCompare(b)
  );
  const uniqueLocations = Array.from(new Set(events.map(getLocationLabel))).sort((a, b) =>
    a.localeCompare(b)
  );
  const uniqueCategories = Array.from(
    new Set(events.map((event) => event.category || "Uncategorized"))
  ).sort((a, b) => a.localeCompare(b));

  // Group events by source, then location
  const groupEventsBySource = () => {
    const grouped = {};

    filteredEvents.forEach((event) => {
      const sourceKey = getSourceLabel(event);
      const locationKey = getLocationLabel(event);

      if (!grouped[sourceKey]) grouped[sourceKey] = {};
      if (!grouped[sourceKey][locationKey]) grouped[sourceKey][locationKey] = [];
      grouped[sourceKey][locationKey].push(event);
    });

    Object.keys(grouped).forEach((source) => {
      Object.keys(grouped[source]).forEach((location) => {
        grouped[source][location].sort(
          (a, b) => getEventTimestamp(a) - getEventTimestamp(b)
        );
      });
    });

    return grouped;
  };

  return (
    <div className="page">
      <header className="header">
        <h1>{APP_CONFIG.appName}</h1>
        <p>Discover events and places in {APP_CONFIG.cityLabel}</p>
      </header>

      <section className="controls">
        <div className="field">
          <label htmlFor="search">Search</label>
          <input
            id="search"
            type="text"
            placeholder="Artist, venue, genre..."
            value={searchText}
            onChange={(event) => setSearchText(event.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="source">Source</label>
          <select
            id="source"
            value={selectedSource}
            onChange={(event) => setSelectedSource(event.target.value)}
          >
            <option value="all">All sources</option>
            {uniqueSources.map((source) => (
              <option key={source} value={source}>{source}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="location">Location</label>
          <select
            id="location"
            value={selectedLocation}
            onChange={(event) => setSelectedLocation(event.target.value)}
          >
            <option value="all">All locations</option>
            {uniqueLocations.map((location) => (
              <option key={location} value={location}>{location}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="category">Genre</label>
          <select
            id="category"
            value={selectedCategory}
            onChange={(event) => setSelectedCategory(event.target.value)}
          >
            <option value="all">All genres</option>
            {uniqueCategories.map((cat) => (
              <option key={cat} value={cat}>
                {CATEGORY_INFO[cat]
                  ? `${CATEGORY_INFO[cat].icon} ${CATEGORY_INFO[cat].label}`
                  : cat}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="dateFrom">From</label>
          <input
            id="dateFrom"
            type="date"
            value={dateFrom}
            onChange={(e) => handleDateFromChange(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="dateTo">To</label>
          <input
            id="dateTo"
            type="date"
            value={dateTo}
            onChange={(e) => handleDateToChange(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="sort">Sort</label>
          <select
            id="sort"
            value={sortMode}
            onChange={(event) => setSortMode(event.target.value)}
          >
            <option value="date-asc">Date (earliest)</option>
            <option value="date-desc">Date (latest)</option>
            <option value="title">Title (A-Z)</option>
          </select>
        </div>
        <button onClick={fetchData} disabled={loading}>
          {loading ? loadingMessage || "Loading..." : "Refresh Data"}
        </button>
      </section>

      {error && (
        <p className={error.startsWith("✓") ? "success" : "error"}>{error}</p>
      )}

      {/* ── Quick-filter bar: date presets + category chips ── */}
      <section className="quick-filters">
        <div className="filter-group">
          <span className="filter-group-label">Wann</span>
          <div className="chip-row">
            {[
              { id: "today",    label: "Heute" },
              { id: "tomorrow", label: "Morgen" },
              { id: "weekend",  label: "Wochenende" },
            ].map(({ id, label }) => (
              <button
                key={id}
                className={`chip-btn date-chip${activeDatePreset === id ? " active" : ""}`}
                onClick={() => applyDatePreset(id)}
              >
                {label}
              </button>
            ))}
            {(dateFrom || dateTo) && (
              <button
                className="chip-btn clear-chip"
                onClick={() => { setDateFrom(""); setDateTo(""); setActiveDatePreset(""); }}
              >
                ✕ Datum
              </button>
            )}
          </div>
        </div>

        <div className="filter-group">
          <span className="filter-group-label">Genre</span>
          <div className="chip-row">
            {Object.entries(CATEGORY_INFO).map(([key, { label, icon }]) => (
              <button
                key={key}
                className={`chip-btn category-chip${selectedCategory === key ? " active" : ""}`}
                onClick={() => setSelectedCategory(selectedCategory === key ? "all" : key)}
              >
                {icon} {label}
              </button>
            ))}
            {selectedCategory !== "all" && (
              <button
                className="chip-btn clear-chip"
                onClick={() => setSelectedCategory("all")}
              >
                ✕ Genre
              </button>
            )}
          </div>
        </div>
      </section>

      <section className="tabs">
        <button
          className={activeTab === "events" ? "active" : ""}
          onClick={() => setActiveTab("events")}
        >
          Events ({events.length})
        </button>
        <button
          className={activeTab === "places" ? "active" : ""}
          onClick={() => setActiveTab("places")}
        >
          Places ({places.length})
        </button>
        <button
          className={activeTab === "admin" ? "active" : ""}
          onClick={() => setActiveTab("admin")}
        >
          Admin
        </button>
      </section>

      <section className="results">
        {activeTab === "events" && (
          <>
            <h2>🎭 Events by Source</h2>
            <p className="results-meta">
              Showing {filteredEvents.length} of {events.length} events
            </p>
            {filteredEvents.length === 0 && !loading ? (
              <p>No events found. Try refreshing.</p>
            ) : (
              <div className="source-sections">
                {Object.entries(groupEventsBySource())
                  .sort(([a], [b]) => a.localeCompare(b))
                  .map(([source, locations]) => (
                    <div key={source} className="source-section">
                      <div className="source-header">
                        <h3
                          className="source-name"
                          role="button"
                          tabIndex={0}
                          onClick={() => setSelectedSource(source)}
                          onKeyDown={(e) => {
                            if (e.key === "Enter" || e.key === " ") {
                              e.preventDefault();
                              setSelectedSource(source);
                            }
                          }}
                        >
                          🧭 <u>{source}</u>
                        </h3>
                      </div>
                      <div className="source-locations">
                        {Object.entries(locations)
                          .sort(([a], [b]) => a.localeCompare(b))
                          .map(([location, sourceEvents]) => (
                            <LocationSlider
                              key={`${source}-${location}`}
                              location={location}
                              events={sourceEvents}
                            />
                          ))}
                      </div>
                    </div>
                  ))}
              </div>
            )}
          </>
        )}

        {activeTab === "places" && (
          <>
            <h2>Places</h2>
            {places.length === 0 && !loading ? (
              <p>No places yet. Try searching.</p>
            ) : (
              <ul>
                {places.map((place) => (
                  <li key={place.id || place.name}>
                    <strong>{place.name}</strong>
                    <span>{place.category || "Unknown"}</span>
                    {place.sourceUrl && (
                      <a href={place.sourceUrl} target="_blank" rel="noreferrer">
                        Source
                      </a>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </>
        )}

        {activeTab === "admin" && (
          <>
            <ScrapingPanel />
            <ScrapedWebsites />
          </>
        )}
      </section>
    </div>
  );
}
