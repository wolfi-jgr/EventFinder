import { getFrontendThemeByMode, toFrontendCssVariables } from "./config";

export const applyFrontendTheme = (mode = "light") => {
  if (typeof document === "undefined") {
    return;
  }

  const root = document.documentElement;
  const vars = toFrontendCssVariables(getFrontendThemeByMode(mode));

  Object.entries(vars).forEach(([key, value]) => {
    root.style.setProperty(key, value);
  });

  root.setAttribute("data-theme", mode);
};
