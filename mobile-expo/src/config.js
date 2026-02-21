import { Platform } from "react-native";
import Constants from "expo-constants";

const fromEnv = process.env.EXPO_PUBLIC_API_BASE;

const getExpoHost = () => {
  const hostUri =
    Constants.expoConfig?.hostUri ||
    Constants.manifest2?.extra?.expoClient?.hostUri ||
    Constants.manifest?.debuggerHost;

  if (!hostUri) {
    return null;
  }

  return hostUri.split(":")[0];
};

const isIpv4 = (value) => /^\d{1,3}(\.\d{1,3}){3}$/.test(value || "");

const isPrivateIpv4 = (value) =>
  /^(10\.|192\.168\.|172\.(1[6-9]|2\d|3[0-1])\.)/.test(value || "");

const uniq = (items) => [...new Set(items.filter(Boolean))];

const getApiBaseCandidates = () => {
  if (fromEnv) {
    return [fromEnv];
  }

  const expoHost = getExpoHost();
  const candidates = [];

  if (Platform.OS === "android") {
    candidates.push("http://10.0.2.2:8080");
  }

  if (isIpv4(expoHost) && isPrivateIpv4(expoHost)) {
    candidates.push(`http://${expoHost}:8080`);
  }

  candidates.push("http://localhost:8080");
  return uniq(candidates);
};

const getApiBase = () => {
  const candidates = getApiBaseCandidates();
  return candidates[0] || "http://localhost:8080";
};

export const API_BASE = getApiBase();
export const API_BASE_CANDIDATES = getApiBaseCandidates();
