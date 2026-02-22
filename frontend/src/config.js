// API Configuration
// In production, VITE_API_BASE is set via environment variable during build
// In development, it defaults to localhost
import { getDefaultApiBase } from "@shared/frontendConfig";

const getApiBase = () => {
  // Check if we're in production and if API_BASE was set during build
  const envApiBase = import.meta.env.VITE_API_BASE;
  
  if (envApiBase) {
    return envApiBase;
  }
  
  // In production without explicit VITE_API_BASE, use relative path
  // This assumes frontend and backend are on same domain (via reverse proxy)
  if (import.meta.env.PROD) {
    return window.location.origin;
  }
  
  // Development fallback
  return getDefaultApiBase();
};

export const API_BASE = getApiBase();

export default {
  API_BASE,
};
