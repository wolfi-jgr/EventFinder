import { StatusBar } from "expo-status-bar";
import { useMemo, useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  Linking,
  Pressable,
  SafeAreaView,
  StyleSheet,
  Text,
  useColorScheme,
  View,
} from "react-native";
import { API_BASE } from "./src/config";
import { fetchEvents, fetchHealth, fetchPlaces, getApiDebugState, runScraper } from "./src/api";
import { APP_CONFIG, getMobileThemeByMode } from "../shared/frontendConfig";

const DEFAULT_COORDS = APP_CONFIG.defaultCoords;

const formatDateTime = (event) => {
  const dateTime = event.startDateTime || (event.startDate ? `${event.startDate}T${event.startTime || "00:00:00"}` : null);
  if (!dateTime) return "";
  const date = new Date(dateTime);
  return date.toLocaleString("de-AT", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
};

const TabButton = ({ active, label, onPress, styles }) => (
  <Pressable style={[styles.tab, active && styles.tabActive]} onPress={onPress}>
    <Text style={[styles.tabLabel, active && styles.tabLabelActive]}>{label}</Text>
  </Pressable>
);

export default function App() {
  const systemColorScheme = useColorScheme();
  const [themeMode, setThemeMode] = useState(systemColorScheme === "light" ? "light" : "dark");
  const [events, setEvents] = useState([]);
  const [places, setPlaces] = useState([]);
  const [activeTab, setActiveTab] = useState("events");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("Tap Refresh to load data");
  const [apiDebug, setApiDebug] = useState(getApiDebugState());

  const mobileTheme = useMemo(() => getMobileThemeByMode(themeMode), [themeMode]);
  const styles = useMemo(() => createStyles(mobileTheme), [mobileTheme]);
  const apiHint = useMemo(() => `API: ${API_BASE}`, []);

  const toggleThemeMode = () => {
    setThemeMode((prev) => (prev === "dark" ? "light" : "dark"));
  };

  const loadData = async () => {
    setLoading(true);
    setMessage("Loading data...");

    try {
      const [eventsResult, placesResult] = await Promise.allSettled([
        fetchEvents(),
        fetchPlaces(DEFAULT_COORDS.lat, DEFAULT_COORDS.lon),
      ]);

      let eventsLoaded = false;
      let placesLoaded = false;
      const problems = [];

      if (eventsResult.status === "fulfilled") {
        setEvents(eventsResult.value);
        eventsLoaded = true;
      } else {
        problems.push(`events: ${eventsResult.reason?.message || "failed"}`);
      }

      if (placesResult.status === "fulfilled") {
        setPlaces(placesResult.value);
        placesLoaded = true;
      } else {
        problems.push(`places: ${placesResult.reason?.message || "failed"}`);
      }

      if (eventsLoaded && placesLoaded) {
        setMessage("Data loaded");
      } else if (eventsLoaded || placesLoaded) {
        setMessage(`Partial data loaded (${problems.join(" | ")})`);
      } else {
        const health = await fetchHealth();
        const healthOk = health?.status === "ok";
        setMessage(
          healthOk
            ? `Backend reachable, but data endpoints failed (${problems.join(" | ")})`
            : `Backend unreachable (${problems.join(" | ")})`
        );
      }
    } catch (error) {
      setMessage(error.message || "Request failed");
    } finally {
      setApiDebug(getApiDebugState());
      setLoading(false);
    }
  };

  const scrapeAndReload = async () => {
    setLoading(true);
    setMessage("Scraping events...");

    try {
      const result = await runScraper();
      await loadData();
      const newEvents = result.newEvents || result.totalEvents || 0;
      setMessage(`Scraping finished. ${newEvents} new events.`);
    } catch (error) {
      setMessage(error.message || "Scraping failed");
      setLoading(false);
    } finally {
      setApiDebug(getApiDebugState());
    }
  };

  const renderEvent = ({ item }) => (
    <View style={styles.card}>
      <Text style={styles.title}>{item.title}</Text>
      {(item.startDateTime || item.startDate) ? (
        <Text style={styles.detail}>📅 {formatDateTime(item)}</Text>
      ) : null}
      {item.location ? <Text style={styles.detail}>📍 {item.location}</Text> : null}
      {item.organizer ? <Text style={styles.detail}>👤 {item.organizer}</Text> : null}
      {item.sourceUrl ? (
        <Pressable onPress={() => Linking.openURL(item.sourceUrl)}>
          <Text style={styles.link}>View details</Text>
        </Pressable>
      ) : null}
    </View>
  );

  const renderPlace = ({ item }) => (
    <View style={styles.card}>
      <Text style={styles.title}>{item.name}</Text>
      {item.category ? <Text style={styles.detail}>{item.category}</Text> : null}
      {item.sourceUrl ? (
        <Pressable onPress={() => Linking.openURL(item.sourceUrl)}>
          <Text style={styles.link}>Source</Text>
        </Pressable>
      ) : null}
    </View>
  );

  const data = activeTab === "events" ? events : places;
  const renderItem = activeTab === "events" ? renderEvent : renderPlace;

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar style={themeMode === "dark" ? "light" : "dark"} />

      <View style={styles.heroCard}>
        <View style={styles.heroTopRow}>
          <Text style={styles.header}>{APP_CONFIG.appName} Mobile</Text>
          <Pressable
            style={styles.themeIconButton}
            onPress={toggleThemeMode}
            accessibilityLabel={themeMode === "dark" ? "Switch to light mode" : "Switch to dark mode"}
          >
            <Text style={styles.themeIconLabel}>{themeMode === "dark" ? "☀" : "🌙"}</Text>
          </Pressable>
        </View>
        <Text style={styles.subheader}>Simple Expo test client ({APP_CONFIG.cityLabel})</Text>
        <Text style={styles.apiHint}>{apiHint}</Text>
        <Text style={styles.apiDebug}>Current base: {apiDebug.currentBase}</Text>
        <Text style={styles.apiDebug}>Last attempt: {apiDebug.lastAttempt}</Text>
        <Text style={styles.apiDebug}>Last success: {apiDebug.lastSuccess}</Text>
      </View>

      <View style={styles.actionsRow}>
        <Pressable style={styles.primaryButton} onPress={loadData} disabled={loading}>
          <Text style={styles.primaryButtonLabel}>Refresh</Text>
        </Pressable>
        <Pressable style={styles.secondaryButton} onPress={scrapeAndReload} disabled={loading}>
          <Text style={styles.secondaryButtonLabel}>Scrape</Text>
        </Pressable>
      </View>

      <Text style={styles.message}>{message}</Text>

      {loading ? <ActivityIndicator style={styles.loader} /> : null}

      <View style={styles.tabs}>
        <TabButton
          active={activeTab === "events"}
          label={`Events (${events.length})`}
          onPress={() => setActiveTab("events")}
          styles={styles}
        />
        <TabButton
          active={activeTab === "places"}
          label={`Places (${places.length})`}
          onPress={() => setActiveTab("places")}
          styles={styles}
        />
      </View>

      <FlatList
        data={data}
        keyExtractor={(item, index) => String(item.id || item.name || index)}
        renderItem={renderItem}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={!loading ? <Text style={styles.empty}>No data yet.</Text> : null}
      />
    </SafeAreaView>
  );
}

const createStyles = (theme) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.background,
    paddingHorizontal: 16,
    paddingTop: 6,
  },
  heroCard: {
    borderWidth: 2,
    borderColor: theme.colors.tabBorder,
    borderRadius: 14,
    backgroundColor: theme.colors.card,
    paddingHorizontal: 12,
    paddingVertical: 10,
    shadowColor: "#000",
    shadowOpacity: 0.16,
    shadowRadius: 0,
    shadowOffset: { width: 4, height: 4 },
    elevation: 3,
  },
  heroTopRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    gap: 12,
  },
  header: {
    fontSize: 26,
    fontWeight: "800",
    marginTop: 2,
    color: theme.colors.textPrimary,
  },
  themeIconButton: {
    width: 40,
    height: 40,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: theme.colors.tabBorder,
    backgroundColor: theme.colors.secondaryButton,
    alignItems: "center",
    justifyContent: "center",
  },
  themeIconLabel: {
    fontSize: 18,
  },
  subheader: {
    color: theme.colors.textSecondary,
    marginTop: 4,
    fontWeight: "600",
  },
  apiHint: {
    color: theme.colors.textMuted,
    marginTop: 6,
    fontSize: 12,
  },
  apiDebug: {
    color: theme.colors.textMuted,
    marginTop: 2,
    fontSize: 11,
  },
  actionsRow: {
    marginTop: 14,
    flexDirection: "row",
    gap: 10,
    flexWrap: "wrap",
  },
  primaryButton: {
    backgroundColor: theme.colors.primaryButton,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: theme.colors.tabBorder,
    paddingHorizontal: 14,
    paddingVertical: 10,
    shadowColor: "#000",
    shadowOpacity: 0.12,
    shadowRadius: 0,
    shadowOffset: { width: 3, height: 3 },
    elevation: 2,
  },
  primaryButtonLabel: {
    color: theme.colors.primaryButtonText,
    fontWeight: "700",
  },
  secondaryButton: {
    backgroundColor: theme.colors.secondaryButton,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: theme.colors.tabBorder,
    paddingHorizontal: 14,
    paddingVertical: 10,
    shadowColor: "#000",
    shadowOpacity: 0.1,
    shadowRadius: 0,
    shadowOffset: { width: 2, height: 2 },
    elevation: 1,
  },
  secondaryButtonLabel: {
    color: theme.colors.secondaryButtonText,
    fontWeight: "700",
  },
  message: {
    marginTop: 12,
    color: theme.colors.textSecondary,
    backgroundColor: theme.colors.card,
    borderWidth: 1,
    borderColor: theme.colors.tabBorder,
    borderRadius: 10,
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  loader: {
    marginTop: 8,
  },
  tabs: {
    flexDirection: "row",
    gap: 8,
    marginTop: 14,
  },
  tab: {
    backgroundColor: theme.colors.card,
    borderWidth: 1,
    borderColor: theme.colors.tabBorder,
    borderRadius: 10,
    paddingHorizontal: 10,
    paddingVertical: 8,
    flex: 1,
    alignItems: "center",
  },
  tabActive: {
    backgroundColor: theme.colors.tabActive,
    borderColor: theme.colors.tabActive,
  },
  tabLabel: {
    color: theme.colors.textPrimary,
    fontWeight: "600",
  },
  tabLabelActive: {
    color: theme.colors.tabActiveText,
  },
  listContent: {
    paddingVertical: 14,
    gap: 10,
  },
  card: {
    backgroundColor: theme.colors.card,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: theme.colors.tabBorder,
    padding: 12,
    shadowColor: "#000",
    shadowOpacity: 0.08,
    shadowRadius: 0,
    shadowOffset: { width: 2, height: 2 },
    elevation: 1,
  },
  title: {
    fontSize: 16,
    fontWeight: "800",
    color: theme.colors.textPrimary,
  },
  detail: {
    marginTop: 4,
    color: theme.colors.textSecondary,
  },
  link: {
    marginTop: 8,
    color: theme.colors.link,
    fontWeight: "600",
  },
  empty: {
    textAlign: "center",
    color: theme.colors.textMuted,
    marginTop: 20,
  },
});
