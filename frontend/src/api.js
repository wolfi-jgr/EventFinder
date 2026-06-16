import { API_BASE } from "./config";

const defaultOptions = {
  credentials: "include",
  headers: {
    "Content-Type": "application/json",
  },
};

export const fetchApi = async (path, options = {}) => {
  const url = path.startsWith("http") ? path : `${API_BASE}${path}`;
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
