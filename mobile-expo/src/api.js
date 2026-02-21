import { API_BASE, API_BASE_CANDIDATES } from "./config";

let resolvedApiBase = API_BASE;
let lastAttempt = "-";
let lastSuccess = "-";

const getBaseCandidates = () => {
  const candidates = [resolvedApiBase, ...API_BASE_CANDIDATES];
  return [...new Set(candidates.filter(Boolean))];
};

const requestJson = async (path, options) => {
  const bases = getBaseCandidates();
  let lastError = null;
  const mergedOptions = {
    ...options,
    headers: {
      "bypass-tunnel-reminder": "true",
      ...(options?.headers || {}),
    },
  };

  for (const base of bases) {
    try {
      const endpoint = `${base}${path}`;
      lastAttempt = endpoint;
      const response = await fetch(endpoint, mergedOptions);
      if (!response.ok) {
        throw new Error(`${path} failed with ${response.status} on ${base}`);
      }

      resolvedApiBase = base;
      lastSuccess = endpoint;
      return response.json();
    } catch (error) {
      lastError = error;
    }
  }

  throw lastError || new Error(`All API base candidates failed for ${path}`);
};

export const fetchEvents = () => requestJson("/api/events");

export const fetchHealth = () => requestJson("/api/health");

export const fetchPlaces = (lat = 48.2082, lon = 16.3738) =>
  requestJson(`/api/places?lat=${lat}&lon=${lon}`);

export const runScraper = () =>
  requestJson("/api/scraping/run", {
    method: "POST",
  });

export const getApiDebugState = () => ({
  currentBase: resolvedApiBase,
  lastAttempt,
  lastSuccess,
  candidates: getBaseCandidates(),
});
