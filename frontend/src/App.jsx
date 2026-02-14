import { useEffect, useState } from "react";
import "./App.css";

const DEFAULT_COORDS = { lat: 52.52, lon: 13.405 };

export default function App() {
  const [coords, setCoords] = useState(DEFAULT_COORDS);
  const [places, setPlaces] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    fetchPlaces();
  }, []);

  const fetchPlaces = async () => {
    setLoading(true);
    setError("");

    try {
      const base = import.meta.env.VITE_API_BASE || "http://localhost:8080";
      const url = `${base}/api/places?lat=${coords.lat}&lon=${coords.lon}`;
      const response = await fetch(url);

      if (!response.ok) {
        throw new Error(`Request failed with ${response.status}`);
      }

      const data = await response.json();
      setPlaces(data);
    } catch (err) {
      setError(err.message || "Request failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <header className="header">
        <h1>EventFinder</h1>
        <p>Find interesting places and events near your location.</p>
      </header>

      <section className="controls">
        <div className="field">
          <label htmlFor="lat">Latitude</label>
          <input
            id="lat"
            type="number"
            step="0.0001"
            value={coords.lat}
            onChange={(event) =>
              setCoords({ ...coords, lat: Number(event.target.value) })
            }
          />
        </div>
        <div className="field">
          <label htmlFor="lon">Longitude</label>
          <input
            id="lon"
            type="number"
            step="0.0001"
            value={coords.lon}
            onChange={(event) =>
              setCoords({ ...coords, lon: Number(event.target.value) })
            }
          />
        </div>
        <button onClick={fetchPlaces} disabled={loading}>
          {loading ? "Loading..." : "Search"}
        </button>
      </section>

      {error && <p className="error">{error}</p>}

      <section className="results">
        <h2>Results</h2>
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
      </section>
    </div>
  );
}
