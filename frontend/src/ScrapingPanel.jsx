import { useState } from "react";
import "./ScrapingPanel.css";
import { API_BASE } from "./config";

export default function ScrapingPanel() {
  const [sources, setSources] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [scrapingResult, setScrapingResult] = useState(null);

  const initializeSources = async () => {
    setLoading(true);
    setMessage("");
    try {
      const response = await fetch(`${API_BASE}/api/sources/initialize`, {
        method: "POST",
      });
      const text = await response.text();
      setMessage(text);
      await loadSources();
    } catch (err) {
      setMessage("Error: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  const loadSources = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE}/api/sources`);
      const data = await response.json();
      setSources(data);
    } catch (err) {
      setMessage("Error loading sources: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  const runScraping = async () => {
    setLoading(true);
    setMessage("Scraping all websites in progress... this may take a minute...");
    setScrapingResult(null);
    try {
      const response = await fetch(`${API_BASE}/api/scraping/rules/run`, {
        method: "POST",
      });
      const result = await response.json();
      setScrapingResult(result);
      const totalNew = result.totalNew || 0;
      const sitesProcessed = result.sitesProcessed || 0;
      if (totalNew > 0) {
        setMessage(`✓ Scraping completed! ${totalNew} new event(s) from ${sitesProcessed} site(s). Switch to the Events tab to see them.`);
      } else {
        setMessage("Scraping completed but no events were found.");
      }
    } catch (err) {
      setMessage("Error: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  const toggleSource = async (id) => {
    try {
      await fetch(`${API_BASE}/api/sources/${id}/toggle`, {
        method: "POST",
      });
      await loadSources();
    } catch (err) {
      setMessage("Error toggling source: " + err.message);
    }
  };

  const fixScrapers = async () => {
    setLoading(true);
    setMessage("Fixing scraper types...");
    try {
      const response = await fetch(`${API_BASE}/api/sources/fix-scrapers`, {
        method: "POST",
      });
      const text = await response.text();
      setMessage(text);
      await loadSources();
    } catch (err) {
      setMessage("Error: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="scraping-panel">
      <h2>🔧 Scraping Admin</h2>

      <div className="actions">
        <button onClick={initializeSources} disabled={loading}>
          Initialize Default Sources
        </button>
        <button onClick={loadSources} disabled={loading}>
          Load Sources
        </button>
        <button onClick={fixScrapers} disabled={loading}>
          Fix Scraper Types
        </button>
        <button onClick={runScraping} disabled={loading || sources.length === 0}>
          Run Scraping
        </button>
      </div>

      {message && <div className="message">{message}</div>}

      {scrapingResult && (
        <div className="scraping-result">
          <h3>Scraping Results</h3>
          <div className="result-stats">
            <div>✅ Success: {scrapingResult.successCount}</div>
            <div>❌ Failed: {scrapingResult.failureCount}</div>
            <div>📦 Total Events: {scrapingResult.totalEvents}</div>
          </div>
          {scrapingResult.errors && scrapingResult.errors.length > 0 && (
            <div className="errors">
              <h4>Errors:</h4>
              {scrapingResult.errors.map((error, index) => (
                <div key={index} className="error-item">
                  {error}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {sources.length > 0 && (
        <div className="sources-list">
          <h3>Sources ({sources.length})</h3>
          {sources.map((source) => (
            <div key={source.id} className="source-item">
              <div className="source-info">
                <strong>{source.name}</strong>
                <span className={`status ${source.isEnabled ? "enabled" : "disabled"}`}>
                  {source.isEnabled ? "✓ Enabled" : "✗ Disabled"}
                </span>
                {source.isSystem && <span className="badge">System</span>}
              </div>
              <div className="source-details">
                <div>URL: {source.url}</div>
                <div>Type: {source.scraperType}</div>
                {source.category && <div>Category: {source.category}</div>}
                {source.lastScrapedAt && (
                  <div>Last scraped: {new Date(source.lastScrapedAt).toLocaleString()}</div>
                )}
                {source.lastErrorMessage && (
                  <div className="error-message">Error: {source.lastErrorMessage}</div>
                )}
              </div>
              <div className="source-actions">
                <button onClick={() => toggleSource(source.id)}>
                  {source.isEnabled ? "Disable" : "Enable"}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
