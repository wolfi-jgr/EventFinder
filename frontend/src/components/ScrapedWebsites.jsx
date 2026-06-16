import { useState, useEffect } from "react";
import { API_BASE } from "../config";
import "./ScrapedWebsites.css";

export default function ScrapedWebsites({ authOptions = {} }) {
  const [websites, setWebsites] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    fetchWebsiteStatus();
  }, []);

  const fetchWebsiteStatus = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await fetch(`${API_BASE}/api/scraping/rules/status`, {
        ...authOptions,
      });
      if (!response.ok) {
        throw new Error(`Failed to fetch website status: ${response.status}`);
      }
      const data = await response.json();
      setWebsites(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message);
      setWebsites([]);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="scraping-websites loading">⏳ Loading websites...</div>;
  }

  return (
    <div className="scraping-websites">
      <div className="websites-header">
        <h2>📡 Scraped Websites</h2>
        <button className="refresh-btn" onClick={fetchWebsiteStatus} title="Refresh status">
          🔄
        </button>
      </div>

      {error && <div className="error-message">❌ {error}</div>}

      {websites.length === 0 ? (
        <p className="no-websites">No websites configured yet.</p>
      ) : (
        <div className="websites-grid">
          {websites.map((site) => (
            <div
              key={site.siteName}
              className={`website-card ${site.enabled ? "enabled" : "disabled"}`}
            >
              <div className="website-header">
                <h3 className="website-name">{site.siteName}</h3>
                <span className={`status-badge ${site.enabled ? "active" : "inactive"}`}>
                  {site.enabled ? "🟢 Active" : "⚫ Inactive"}
                </span>
              </div>

              <div className="website-info">
                <div className="info-item">
                  <span className="info-label">Mode:</span>
                  <span className="info-value">
                    {site.extractionMode === "CSS_SELECTOR" ? "🎯 Selector" : "⚙️ Regex"}
                  </span>
                </div>

                <div className="info-item">
                  <span className="info-label">Events:</span>
                  <span className="info-value">{site.eventCount || 0}</span>
                </div>

                <div className="info-item">
                  <span className="info-label">Cached:</span>
                  <span className="info-value">
                    {site.hasCachedHtml ? "✅ Yes" : "❌ No"}
                  </span>
                </div>

                {site.aiEnabled && (
                  <div className="info-item ai-enabled">
                    <span className="info-label">AI Fallback:</span>
                    <span className="info-value">🤖 Enabled</span>
                  </div>
                )}
              </div>

              <div className="website-actions">
                <a
                  href={site.baseUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="action-button action-visit"
                  title="Visit website"
                >
                  🔗 Visit
                </a>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
