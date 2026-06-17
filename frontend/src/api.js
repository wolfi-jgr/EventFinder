import { API_BASE } from "./config";

const TOKEN_KEY = "authToken";

// ============ Token Management ============
export const setToken = (token) => {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  }
};

export const getToken = () => {
  return localStorage.getItem(TOKEN_KEY);
};

export const removeToken = () => {
  localStorage.removeItem(TOKEN_KEY);
};

export const isAuthenticated = () => {
  return !!getToken();
};

// ============ Fetch Utilities ============
const getDefaultOptions = () => {
  const options = {
    headers: {
      "Content-Type": "application/json",
    },
  };

  // Add JWT token to Authorization header if available
  const token = getToken();
  if (token) {
    options.headers.Authorization = `Bearer ${token}`;
  }

  return options;
};

export const fetchApi = async (path, options = {}) => {
  const url = path.startsWith("http") ? path : `${API_BASE}${path}`;
  const defaultOptions = getDefaultOptions();
  const mergedOptions = {
    ...defaultOptions,
    ...options,
    headers: {
      ...defaultOptions.headers,
      ...(options.headers || {}),
    },
  };
  return fetch(url, mergedOptions);
};

export const jsonFetch = async (path, options = {}) => {
  const response = await fetchApi(path, options);
  const contentType = response.headers.get("Content-Type") || "";
  const body = contentType.includes("application/json") ? await response.json() : null;
  if (!response.ok) {
    const error = body?.message || body?.error || response.statusText || "Request failed";
    const err = new Error(error);
    err.status = response.status;
    throw err;
  }
  return body;
};
