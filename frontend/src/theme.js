import { FRONTEND_THEME, toFrontendCssVariables } from "@shared/frontendConfig";

export const applyFrontendTheme = () => {
  if (typeof document === "undefined") {
    return;
  }

  const root = document.documentElement;
  const vars = toFrontendCssVariables(FRONTEND_THEME);

  Object.entries(vars).forEach(([key, value]) => {
    root.style.setProperty(key, value);
  });
};
