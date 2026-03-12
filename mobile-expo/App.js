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
  View,
} from "react-native";
import { API_BASE } from "./src/config";
import { fetchEvents, fetchHealth, fetchPlaces, getApiDebugState, runScraper } from "./src/api";
import { APP_CONFIG, MOBILE_THEME } from "../shared/frontendConfig";

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

const TabButton = ({ active, label, onPress }) => (
  <Pressable style={[styles.tab, active && styles.tabActive]} onPress={onPress}>
    <Text style={[styles.tabLabel, active && styles.tabLabelActive]}>{label}</Text>
  </Pressable>
);

export default function App() {
  const [events, setEvents] = useState([]);
  const [places, setPlaces] = useState([]);
  const [activeTab, setActiveTab] = useState("events");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("Tap Refresh to load data");
  const [apiDebug, setApiDebug] = useState(getApiDebugState());

  const apiHint = useMemo(() => `API: ${API_BASE}`, []);

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
      <StatusBar style="dark" />

      <Text style={styles.header}>{APP_CONFIG.appName} Mobile</Text>
      <Text style={styles.subheader}>Simple Expo test client ({APP_CONFIG.cityLabel})</Text>
      <Text style={styles.apiHint}>{apiHint}</Text>
      <Text style={styles.apiDebug}>Current base: {apiDebug.currentBase}</Text>
      <Text style={styles.apiDebug}>Last attempt: {apiDebug.lastAttempt}</Text>
      <Text style={styles.apiDebug}>Last success: {apiDebug.lastSuccess}</Text>

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
        />
        <TabButton
          active={activeTab === "places"}
          label={`Places (${places.length})`}
          onPress={() => setActiveTab("places")}
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

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: MOBILE_THEME.colors.background,
    paddingHorizontal: 16,
  },
  header: {
    fontSize: 26,
    fontWeight: "700",
    marginTop: 12,
  },
  subheader: {
    color: MOBILE_THEME.colors.textMuted,
    marginTop: 4,
  },
  apiHint: {
    color: MOBILE_THEME.colors.textMuted,
    marginTop: 6,
    fontSize: 12,
  },
  apiDebug: {
    color: MOBILE_THEME.colors.textMuted,
    marginTop: 2,
    fontSize: 11,
  },
  actionsRow: {
    marginTop: 14,
    flexDirection: "row",
    gap: 10,
  },
  primaryButton: {
    backgroundColor: MOBILE_THEME.colors.primaryButton,
    borderRadius: 8,
    paddingHorizontal: 14,
    paddingVertical: 10,
  },
  primaryButtonLabel: {
    color: MOBILE_THEME.colors.primaryButtonText,
    fontWeight: "600",
  },
  secondaryButton: {
    backgroundColor: MOBILE_THEME.colors.secondaryButton,
    borderRadius: 8,
    paddingHorizontal: 14,
    paddingVertical: 10,
  },
  secondaryButtonLabel: {
    color: MOBILE_THEME.colors.secondaryButtonText,
    fontWeight: "600",
  },
  message: {
    marginTop: 12,
    color: MOBILE_THEME.colors.textSecondary,
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
    borderWidth: 1,
    borderColor: MOBILE_THEME.colors.tabBorder,
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  tabActive: {
    backgroundColor: MOBILE_THEME.colors.tabActive,
    borderColor: MOBILE_THEME.colors.tabActive,
  },
  tabLabel: {
    color: MOBILE_THEME.colors.textPrimary,
    fontWeight: "600",
  },
  tabLabelActive: {
    color: MOBILE_THEME.colors.tabActiveText,
  },
  listContent: {
    paddingVertical: 14,
    gap: 10,
  },
  card: {
    backgroundColor: MOBILE_THEME.colors.card,
    borderRadius: 10,
    padding: 12,
  },
  title: {
    fontSize: 16,
    fontWeight: "700",
  },
  detail: {
    marginTop: 4,
    color: MOBILE_THEME.colors.textSecondary,
  },
  link: {
    marginTop: 8,
    color: MOBILE_THEME.colors.link,
    fontWeight: "600",
  },
  empty: {
    textAlign: "center",
    color: MOBILE_THEME.colors.textMuted,
    marginTop: 20,
  },
});
