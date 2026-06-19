// Frontend configuration entry that uses the centralized shared config
// and resolves `API_BASE` for local development and production builds.
// API Configuration
// In production, `VITE_API_BASE` is set at build time. For local development
// the code falls back to a localhost URL constructed from `API_CONFIG`.

const getApiBase = () => {
  const envApiBase = import.meta.env.VITE_API_BASE;

  if (envApiBase) return envApiBase;

  // In production without explicit VITE_API_BASE, use current origin
  if (import.meta.env.PROD) return window.location.origin;

  // Development fallback
  return getDefaultApiBase();
};

export const API_BASE = getApiBase();

export default {
  API_BASE,
};


export const APP_CONFIG = {
  appName: "EventFinder",
  cityLabel: "Vienna",
  defaultCoords: {
    lat: 48.2082,
    lon: 16.3738,
  },
};

export const API_CONFIG = {
  defaultProtocol: "http",
  defaultHost: "localhost",
  defaultPort: 8080,
};

const lightColors = {
  textPrimary: "#14213d",
  textMuted: "#5f6b82",
  textSubtle: "#3b4a63",
  textSuccess: "#1b8a5a",
  textError: "#d93025",
  backgroundPage: "#f9f9f9",
  backgroundPageAccent: "#ececec",
  backgroundCard: "#ffffff",
  borderSoft: "#e5e5e5",
  accent: "#ff0000",
  accentHover: "#cc0000",
  accentSoft: "#ffe5e5",
  accentSoftHover: "#ffd1d1",
};

const darkColors = {
  textPrimary: "#f1f1f1",
  textMuted: "#f1f1f1",
  textSubtle: "#d7d7d7",
  textSuccess: "#34a853",
  textError: "#ea4335",
  backgroundPage: "#0f0f0f",
  backgroundPageAccent: "#1a1a1a",
  backgroundCard: "#1f1f1f",
  borderSoft: "#2f2f2f",
  accent: "#ff3d3d",
  accentHover: "#ff6969",
  accentSoft: "#3a1d1d",
  accentSoftHover: "#512424",
};

export const FRONTEND_THEME = {
  fontFamily: '"Space Grotesk", "Segoe UI", sans-serif',
  colors: lightColors,
};

export const FRONTEND_THEME_DARK = {
  fontFamily: '"Space Grotesk", "Segoe UI", sans-serif',
  colors: darkColors,
};

export const MOBILE_THEME_LIGHT = {
  colors: {
    background: "#f9f9f9",
    card: "#ffffff",
    textPrimary: "#181818",
    textSecondary: "#3c4043",
    textMuted: "#60656b",
    primaryButton: "#ff0000",
    primaryButtonText: "#ffffff",
    secondaryButton: "#ececec",
    secondaryButtonText: "#181818",
    tabBorder: "#dedede",
    tabActive: "#181818",
    tabActiveText: "#ffffff",
    link: "#cc0000",
  },
};

export const MOBILE_THEME_DARK = {
  colors: {
    background: "#0f0f0f",
    card: "#1f1f1f",
    textPrimary: "#f1f1f1",
    textSecondary: "#d7d7d7",
    textMuted: "#aaaaaa",
    primaryButton: "#ff3d3d",
    primaryButtonText: "#ffffff",
    secondaryButton: "#2f2f2f",
    secondaryButtonText: "#841919",
    tabBorder: "#3a3a3a",
    tabActive: "#ff3d3d",
    tabActiveText: "#ffffff",
    link: "#ff6969",
  },
};

export const MOBILE_THEME = MOBILE_THEME_DARK;

export const getMobileThemeByMode = (mode = "dark") =>
  mode === "light" ? MOBILE_THEME_LIGHT : MOBILE_THEME_DARK;

export const getFrontendThemeByMode = (mode = "light") =>
  mode === "dark" ? FRONTEND_THEME_DARK : FRONTEND_THEME;

export const toFrontendCssVariables = (theme = FRONTEND_THEME) => ({
  "--ef-font-family": theme.fontFamily,
  "--ef-text-primary": theme.colors.textPrimary,
  "--ef-text-muted": theme.colors.textMuted,
  "--ef-text-subtle": theme.colors.textSubtle,
  "--ef-text-success": theme.colors.textSuccess,
  "--ef-text-error": theme.colors.textError,
  "--ef-bg-page": theme.colors.backgroundPage,
  "--ef-bg-page-accent": theme.colors.backgroundPageAccent,
  "--ef-bg-card": theme.colors.backgroundCard,
  "--ef-border-soft": theme.colors.borderSoft,
  "--ef-accent": theme.colors.accent,
  "--ef-accent-hover": theme.colors.accentHover,
  "--ef-accent-soft": theme.colors.accentSoft,
  "--ef-accent-soft-hover": theme.colors.accentSoftHover,
});

export const getDefaultApiBase = () =>
  `${API_CONFIG.defaultProtocol}://${API_CONFIG.defaultHost}:${API_CONFIG.defaultPort}`;
