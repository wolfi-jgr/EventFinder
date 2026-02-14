import { useEffect, useState } from "react";
import "./App.css";
import ScrapingPanel from "./ScrapingPanel";

const DEFAULT_COORDS = { lat: 48.2082, lon: 16.3738 }; // Vienna

export default function App() {
  const [coords, setCoords] = useState(DEFAULT_COORDS);
  const [places, setPlaces] = useState([]);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [activeTab, setActiveTab] = useState("events"); // "places", "events", or "admin"

  // Load data on initial mount
  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    setError("");

    try {
      const base = import.meta.env.VITE_API_BASE || "http://localhost:8080";
      
      // Fetch places
      const placesUrl = `${base}/api/places?lat=${coords.lat}&lon=${coords.lon}`;
      const placesResponse = await fetch(placesUrl);
      if (!placesResponse.ok) {
        throw new Error(`Places request failed with ${placesResponse.status}`);
      }
      const placesData = await placesResponse.json();
      setPlaces(placesData);

      // Fetch all events (including scraped ones)
      const eventsUrl = `${base}/api/events`;
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
    }
  };

  const formatDateTime = (dateTime) => {
    if (!dateTime) return "";
    const date = new Date(dateTime);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="page">
      <header className="header">
        <h1>EventFinder</h1>
        <p>Discover events and places in Vienna</p>
      </header>

      <section className="controls">
        <button onClick={fetchData} disabled={loading}>
          {loading ? "Loading..." : "Refresh Data"}
        </button>
      </section>

      {error && <p className="error">{error}</p>}

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
            <h2>Events</h2>
            {events.length === 0 && !loading ? (
              <p>No events found. Try searching.</p>
            ) : (
              <ul className="event-list">
                {events.map((event) => (
                  <li key={event.id} className="event-item">
                    <div className="event-header">
                      <strong>{event.title}</strong>
                      {event.category && <span className="badge">{event.category}</span>}
                    </div>
                    {event.description && (
                      <p className="event-description">{event.description}</p>
                    )}
                    <div className="event-details">
                      <div>📅 {formatDateTime(event.startDateTime)}</div>
                      {event.location && <div>📍 {event.location}</div>}
                      {event.organizer && <div>👤 {event.organizer}</div>}
                      {event.price && <div>💰 {event.price}</div>}
                    </div>
                    {event.sourceUrl && (
                      <a href={event.sourceUrl} target="_blank" rel="noreferrer" className="event-link">
                        View Details →
                      </a>
                    )}
                  </li>
                ))}
              </ul>
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

        {activeTab === "admin" && <ScrapingPanel />}
      </section>
    </div>
  );
}
