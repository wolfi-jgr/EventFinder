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
  androidEmulatorHost: "10.0.2.2",
};

const commonColors = {
  textPrimary: "#0d0d0d",
  textMuted: "#666",
  textSubtle: "#555",
  textSuccess: "#2e7d32",
  textError: "#b00020",
  backgroundPage: "#f4f0ea",
  backgroundPageAccent: "#ffe6c4",
  backgroundCard: "#ffffff",
  borderSoft: "#ddd",
  accent: "#ff8a3d",
  accentHover: "#ff7020",
  accentSoft: "#ffe6c4",
  accentSoftHover: "#ffd9a6",
};

export const FRONTEND_THEME = {
  fontFamily: '"Space Grotesk", "Segoe UI", sans-serif',
  colors: commonColors,
};

export const MOBILE_THEME = {
  colors: {
    background: "#f7f7f7",
    card: "#ffffff",
    textPrimary: "#111827",
    textSecondary: "#374151",
    textMuted: "#6b7280",
    primaryButton: "#1d4ed8",
    primaryButtonText: "#ffffff",
    secondaryButton: "#e5e7eb",
    secondaryButtonText: "#111827",
    tabBorder: "#d1d5db",
    tabActive: "#111827",
    tabActiveText: "#ffffff",
    link: "#2563eb",
  },
};

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
