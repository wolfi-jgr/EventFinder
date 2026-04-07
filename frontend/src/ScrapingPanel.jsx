import { useState, useEffect } from "react";
import "./ScrapingPanel.css";
import { API_BASE } from "./config";

export default function ScrapingPanel() {
  const [sites, setSites] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [scrapingResult, setScrapingResult] = useState(null);

  useEffect(() => {
    loadSites();
  }, []);

  const loadSites = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE}/api/scraping/rules/status`);
      if (response.ok) {
        const data = await response.json();
        setSites(data);
      }
    } catch (err) {
      setMessage("Error loading sites: " + err.message);
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
      
      let totalNew = 0;
      let sitesProcessed = 0;
      
      if (Array.isArray(result)) {
        totalNew = result.reduce((sum, r) => sum + (r.newEvents || 0), 0);
        sitesProcessed = result.filter(r => r.newEvents > 0).length;
      } else {
        totalNew = result.totalNew || result.newEvents || 0;
        sitesProcessed = result.sitesProcessed || 1;
      }
      
      if (totalNew > 0) {
        setMessage(`✓ Scraping completed! ${totalNew} new event(s) from ${sitesProcessed} site(s). Switch to the Events tab to see them.`);
      } else {
        setMessage("Scraping completed but no new events were found.");
      }
      
      // Refresh sites list
      await loadSites();
    } catch (err) {
      setMessage("Error: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  const scrapeSite = async (siteName) => {
    setLoading(true);
    setMessage(`Scraping ${siteName}...`);
    setScrapingResult(null);
    try {
      console.log(`Initiating scraping for site: ${siteName}`);
      const response = await fetch(`${API_BASE}/api/scraping/rules/site?siteName=${encodeURIComponent(siteName)}`, {
        method: "POST",
      });
      const result = await response.json();
      
      if (result.error) {
        setMessage(`Error scraping ${siteName}: ${result.error}`);
      } else {
        setScrapingResult(result);
        const newEventsCount = result.newEvents || 0;
        setMessage(`✓ ${siteName}: ${newEventsCount} new event(s)`);
        await loadSites();
      }
    } catch (err) {
      setMessage("Error: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  const clearCache = async (siteName) => {
    try {
      await fetch(`${API_BASE}/api/scraping/cache?siteName=${encodeURIComponent(siteName)}`, {
      method: "DELETE",
      });
      setMessage(`Cache cleared for ${siteName}`);
      await loadSites();
    } catch (err) {
      setMessage("Error clearing cache: " + err.message);
    }
  };

  const clearEvents = async (siteName) => {
    const confirmed = window.confirm(
      `Delete all scraped events for ${siteName}? This cannot be undone.`
    );
    if (!confirmed) {
      return;
    }

    setLoading(true);
    setMessage(`Deleting events for ${siteName}...`);
    setScrapingResult(null);
    try {
      const response = await fetch(
        `${API_BASE}/api/scraping/events/site?siteName=${encodeURIComponent(siteName)}`,
        { method: "DELETE" }
      );
      const result = await response.json();

      if (!response.ok || result.error) {
        setMessage(`Error deleting events for ${siteName}: ${result.error || "Unknown error"}`);
      } else {
        setMessage(`✓ ${siteName}: deleted ${result.deleted || 0} event(s)`);
        await loadSites();
      }
    } catch (err) {
      setMessage("Error deleting events: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  const syncRules = async () => {
    setLoading(true);
    setMessage("Syncing scrape rules from config...");
    setScrapingResult(null);
    try {
      const response = await fetch(`${API_BASE}/api/scraping/rules/sync`, {
        method: "POST",
      });
      const result = await response.json();

      if (!response.ok || result.error) {
        setMessage(`Error syncing rules: ${result.error || "Unknown error"}`);
        return;
      }

      setMessage(
        `✓ Rules synced: ${result.inserted || 0} inserted, ${result.updated || 0} updated, ${result.deleted || 0} deleted.`
      );
      await loadSites();
    } catch (err) {
      setMessage("Error syncing rules: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="scraping-panel">
      <h2>🔧 Scraping Admin</h2>

      <div className="actions">
        <button onClick={runScraping} disabled={loading || sites.length === 0}>
          {loading ? "Scraping..." : "Run All Scrapers"}
        </button>
        <button onClick={syncRules} disabled={loading}>
          Sync Rules
        </button>
        <button onClick={loadSites} disabled={loading}>
          Refresh Status
        </button>
      </div>

      {message && <div className="message">{message}</div>}

      {scrapingResult && (
        <div className="scraping-result">
          <h3>Scraping Results</h3>
          <div className="result-stats">
            {Array.isArray(scrapingResult) ? (
              scrapingResult.map((result) => (
                <div key={result.siteName}>
                  <strong>{result.siteName}</strong>: {result.newEvents || 0} new events
                </div>
              ))
            ) : (
              <>
                <div>📦 New Events: {scrapingResult.newEvents || 0}</div>
                {scrapingResult.eventCount !== undefined && (
                  <div>📊 Total: {scrapingResult.eventCount}</div>
                )}
              </>
            )}
          </div>
        </div>
      )}

      {sites.length > 0 && (
        <div className="sources-list">
          <h3>Configured Scraping Sites ({sites.length})</h3>
          {sites.map((site) => (
            <div key={site.siteName} className="source-item">
              <div className="source-info">
                <strong>{site.siteName}</strong>
                <span className={`status ${site.enabled ? "enabled" : "disabled"}`}>
                  {site.enabled ? "✓ Enabled" : "✗ Disabled"}
                </span>
              </div>
              <div className="source-details">
                <div>Base URL: {site.baseUrl}</div>
                <div>Mode: {site.extractionMode}</div>
                <div>Events: {site.eventCount || 0}</div>
                {site.hasCachedHtml && <div className="cached">📦 Has cached HTML</div>}
              </div>
              <div className="source-actions">
                <button onClick={() => scrapeSite(site.siteName)} disabled={loading}>
                  Scrape Now
                </button>
                <button onClick={() => clearEvents(site.siteName)} disabled={loading}>
                  Clear Events
                </button>
                {site.hasCachedHtml && (
                  <button onClick={() => clearCache(site.siteName)} disabled={loading}>
                    Clear Cache
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
