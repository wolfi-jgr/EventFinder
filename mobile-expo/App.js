import { StatusBar } from "expo-status-bar";
import { useMemo, useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  Linking,
  Pressable,
  RefreshControl,
  ScrollView,
  SafeAreaView,
  StyleSheet,
  Text,
  useColorScheme,
  View,
} from "react-native";
import { API_BASE } from "./src/config";
import { fetchEvents, fetchHealth, fetchPlaces, getApiDebugState } from "./src/api";
import { APP_CONFIG, getMobileThemeByMode } from "../shared/frontendConfig";

const DEFAULT_COORDS = APP_CONFIG.defaultCoords;
const DAY_FILTERS = ["all", "today", "tomorrow", "weekend"];

const toEventDate = (event) => {
  const dateTime = event.startDateTime || (event.startDate ? `${event.startDate}T${event.startTime || "00:00:00"}` : null);
  if (!dateTime) return null;

  const parsed = new Date(dateTime);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
};

const normalizeCategory = (value) => {
  if (!value) return "other";
  return String(value).trim().toLowerCase();
};

const dayFilterLabel = (value) => {
  if (value === "all") return "All Days";
  if (value === "today") return "Today";
  if (value === "tomorrow") return "Tomorrow";
  return "Weekend";
};

const categoryFilterLabel = (value) => {
  if (value === "all") return "All Genres";
  if (value === "other") return "Other";
  return value.charAt(0).toUpperCase() + value.slice(1);
};

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
  <Pressable style={[styles.segmentButton, active && styles.segmentButtonActive]} onPress={onPress}>
    <Text style={[styles.segmentLabel, active && styles.segmentLabelActive]}>{label}</Text>
  </Pressable>
);

const StatPill = ({ label, value, styles }) => (
  <View style={styles.statPill}>
    <Text style={styles.statLabel}>{label}</Text>
    <Text style={styles.statValue}>{value}</Text>
  </View>
);

const toneFromMessage = (message, loading) => {
  if (loading) return "loading";
  if (!message) return "neutral";

  const lower = message.toLowerCase();
  if (lower.includes("failed") || lower.includes("unreachable")) return "error";
  if (lower.includes("loaded") || lower.includes("finished")) return "success";
  return "neutral";
};

export default function App() {
  const systemColorScheme = useColorScheme();
  const [themeMode, setThemeMode] = useState(systemColorScheme === "light" ? "light" : "dark");
  const [events, setEvents] = useState([]);
  const [places, setPlaces] = useState([]);
  const [activeTab, setActiveTab] = useState("events");
  const [dayFilter, setDayFilter] = useState("all");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [apiDebug, setApiDebug] = useState(getApiDebugState());
  const [lastUpdated, setLastUpdated] = useState(null);
  const [hideMessage, setHideMessage] = useState(false);

  const mobileTheme = useMemo(() => getMobileThemeByMode(themeMode), [themeMode]);
  const styles = useMemo(() => createStyles(mobileTheme), [mobileTheme]);
  const apiHint = useMemo(() => `Backend: ${API_BASE}`, []);
  const statusTone = useMemo(() => toneFromMessage(message, loading), [message, loading]);
  const showMessage = useMemo(() => statusTone === "error" && Boolean(message) && !hideMessage, [statusTone, message, hideMessage]);
  const lastSyncLabel = useMemo(() => {
    if (!lastUpdated) return "No sync yet";
    return `Updated ${lastUpdated.toLocaleTimeString("de-AT", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    })}`;
  }, [lastUpdated]);

  const categoryOptions = useMemo(() => {
    const categories = Array.from(
      new Set(
        events
          .map((item) => normalizeCategory(item.category))
          .filter(Boolean)
      )
    ).sort();

    return ["all", ...categories];
  }, [events]);

  const filteredEvents = useMemo(() => {
    const now = new Date();
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const startOfTomorrow = new Date(startOfToday);
    startOfTomorrow.setDate(startOfTomorrow.getDate() + 1);
    const startOfDayAfterTomorrow = new Date(startOfTomorrow);
    startOfDayAfterTomorrow.setDate(startOfDayAfterTomorrow.getDate() + 1);

    return events.filter((event) => {
      const eventDate = toEventDate(event);
      const eventCategory = normalizeCategory(event.category);

      if (categoryFilter !== "all" && categoryFilter !== eventCategory) {
        return false;
      }

      if (dayFilter === "all") return true;
      if (!eventDate) return false;

      if (dayFilter === "today") {
        return eventDate >= startOfToday && eventDate < startOfTomorrow;
      }

      if (dayFilter === "tomorrow") {
        return eventDate >= startOfTomorrow && eventDate < startOfDayAfterTomorrow;
      }

      const weekday = eventDate.getDay();
      return weekday === 5 || weekday === 6 || weekday === 0;
    });
  }, [events, dayFilter, categoryFilter]);

  const toggleThemeMode = () => {
    setThemeMode((prev) => (prev === "dark" ? "light" : "dark"));
  };

  const loadData = async () => {
    setLoading(true);
    setHideMessage(false);
    setMessage("");

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
        setMessage("");
        setLastUpdated(new Date());
      } else if (eventsLoaded || placesLoaded) {
        setMessage(`Partial data loaded (${problems.join(" | ")})`);
        setLastUpdated(new Date());
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

  const renderEvent = ({ item }) => (
    <View style={styles.dataCard}>
      <View style={styles.cardHeaderRow}>
        <Text style={styles.title} numberOfLines={2}>{item.title}</Text>
        {item.category ? (
          <View style={styles.categoryChip}>
            <Text style={styles.categoryChipText}>{item.category}</Text>
          </View>
        ) : null}
      </View>
      {(item.startDateTime || item.startDate) ? (
        <Text style={styles.detail}>Date: {formatDateTime(item)}</Text>
      ) : null}
      {item.location ? <Text style={styles.detail}>Venue: {item.location}</Text> : null}
      {item.organizer ? <Text style={styles.detail}>Host: {item.organizer}</Text> : null}
      {item.sourceUrl ? (
        <Pressable style={styles.linkCta} onPress={() => Linking.openURL(item.sourceUrl)}>
          <Text style={styles.linkCtaLabel}>View details</Text>
        </Pressable>
      ) : null}
    </View>
  );

  const renderPlace = ({ item }) => (
    <View style={styles.dataCard}>
      <Text style={styles.title}>{item.name}</Text>
      {item.category ? <Text style={styles.detail}>{item.category}</Text> : null}
      {item.sourceUrl ? (
        <Pressable style={styles.linkCta} onPress={() => Linking.openURL(item.sourceUrl)}>
          <Text style={styles.linkCtaLabel}>Source</Text>
        </Pressable>
      ) : null}
    </View>
  );

  const data = activeTab === "events" ? filteredEvents : places;
  const renderItem = activeTab === "events" ? renderEvent : renderPlace;

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar style={themeMode === "dark" ? "light" : "dark"} />

      <View style={styles.backgroundTintTop} />
      <View style={styles.backgroundTintBottom} />

      <FlatList
        data={data}
        keyExtractor={(item, index) => String(item.id || item.name || index)}
        renderItem={renderItem}
        contentContainerStyle={styles.listContent}
        refreshControl={
          <RefreshControl
            refreshing={loading}
            onRefresh={loadData}
            tintColor={mobileTheme.colors.primaryButton}
            colors={[mobileTheme.colors.primaryButton]}
          />
        }
        ListHeaderComponent={
          <View style={styles.listHeaderWrap}>
            <View style={styles.heroCard}>
              <View style={styles.heroTopRow}>
                <View style={styles.heroTitleGroup}>
                  <View style={styles.titleInlineRow}>
                    <Text style={styles.header}>{APP_CONFIG.appName}</Text>
                  </View>
                  <Text style={styles.subheader}>Live guide for {APP_CONFIG.cityLabel}</Text>
                </View>
                <Pressable
                  style={styles.themeIconButton}
                  onPress={toggleThemeMode}
                  accessibilityLabel={themeMode === "dark" ? "Switch to light mode" : "Switch to dark mode"}
                >
                  <Text style={styles.themeIconLabel}>{themeMode === "dark" ? "Light" : "Dark"}</Text>
                </Pressable>
              </View>

              <View style={styles.statsRow}>
                <StatPill label="Events" value={events.length} styles={styles} />
                <StatPill label="Places" value={places.length} styles={styles} />
              </View>

              <Text style={styles.metaCaption}>{lastSyncLabel}</Text>
              {/* <Text style={styles.apiHint} numberOfLines={1}>{apiHint}</Text> */}
              {/* <Text style={styles.apiDebug} numberOfLines={1}>Last success: {apiDebug.lastSuccess || "-"}</Text> */}
            </View>

            {showMessage ? (
              <View style={[styles.messageBar, styles[`messageBar_${statusTone}`]]}>
                <Text style={styles.message}>{message}</Text>
                <Pressable style={styles.messageCloseButton} onPress={() => setHideMessage(true)}>
                  <Text style={styles.messageCloseButtonLabel}>Close</Text>
                </Pressable>
              </View>
            ) : null}

            <View style={styles.segmentWrap}>
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

            {activeTab === "events" ? (
              <View style={styles.filtersWrap}>
                <Text style={styles.filtersHeading}>Filters</Text>

                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filterRowContent}>
                  {DAY_FILTERS.map((value) => {
                    const active = dayFilter === value;
                    return (
                      <Pressable
                        key={`day-${value}`}
                        style={[styles.filterChip, active && styles.filterChipActive]}
                        onPress={() => setDayFilter(value)}
                      >
                        <Text style={[styles.filterChipLabel, active && styles.filterChipLabelActive]}>
                          {dayFilterLabel(value)}
                        </Text>
                      </Pressable>
                    );
                  })}
                </ScrollView>

                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filterRowContent}>
                  {categoryOptions.map((value) => {
                    const active = categoryFilter === value;
                    return (
                      <Pressable
                        key={`cat-${value}`}
                        style={[styles.filterChip, active && styles.filterChipActive]}
                        onPress={() => setCategoryFilter(value)}
                      >
                        <Text style={[styles.filterChipLabel, active && styles.filterChipLabelActive]}>
                          {categoryFilterLabel(value)}
                        </Text>
                      </Pressable>
                    );
                  })}
                </ScrollView>

                <Text style={styles.filterSummary}>Showing {filteredEvents.length} of {events.length} events</Text>
              </View>
            ) : null}

            {loading ? <ActivityIndicator style={styles.loader} /> : null}
          </View>
        }
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
  backgroundTintTop: {
    position: "absolute",
    top: -120,
    right: -90,
    width: 240,
    height: 240,
    borderRadius: 240,
    backgroundColor: theme.colors.primaryButton,
    opacity: theme.colors.background === "#0f0f0f" ? 0.14 : 0.08,
  },
  backgroundTintBottom: {
    position: "absolute",
    bottom: -120,
    left: -90,
    width: 220,
    height: 220,
    borderRadius: 220,
    backgroundColor: theme.colors.secondaryButton,
    opacity: theme.colors.background === "#0f0f0f" ? 0.2 : 0.45,
  },
  heroCard: {
    borderWidth: 1,
    borderColor: theme.colors.tabBorder,
    borderRadius: 20,
    backgroundColor: theme.colors.card,
    paddingHorizontal: 14,
    paddingVertical: 14,
    shadowColor: "#000",
    shadowOpacity: 0.16,
    shadowRadius: 14,
    shadowOffset: { width: 0, height: 8 },
    elevation: 5,
  },
  heroTopRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: 12,
  },
  titleInlineRow: {
    flexDirection: "row",
    alignItems: "center",
  },
  heroTitleGroup: {
    flex: 1,
  },
  header: {
    fontSize: 33,
    fontWeight: "900",
    color: theme.colors.textPrimary,
    letterSpacing: -0.6,
  },
  themeIconButton: {
    minWidth: 68,
    height: 34,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: theme.colors.tabBorder,
    backgroundColor: theme.colors.background,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 10,
  },
  themeIconLabel: {
    fontSize: 12,
    fontWeight: "800",
    color: theme.colors.textPrimary,
    textTransform: "uppercase",
    letterSpacing: 0.5,
  },
  subheader: {
    color: theme.colors.textSecondary,
    marginTop: 2,
    fontWeight: "600",
    fontSize: 14,
  },
  statsRow: {
    marginTop: 14,
    flexDirection: "row",
    gap: 8,
  },
  statPill: {
    flex: 1,
    borderRadius: 12,
    backgroundColor: theme.colors.background,
    borderWidth: 1,
    borderColor: theme.colors.tabBorder,
    paddingVertical: 8,
    paddingHorizontal: 8,
  },
  statLabel: {
    color: theme.colors.textMuted,
    fontSize: 11,
    fontWeight: "700",
    textTransform: "uppercase",
    letterSpacing: 0.4,
  },
  statValue: {
    marginTop: 4,
    color: theme.colors.textPrimary,
    fontSize: 18,
    fontWeight: "900",
  },
  metaCaption: {
    color: theme.colors.textSecondary,
    marginTop: 10,
    fontSize: 12,
    fontWeight: "600",
  },
  apiHint: {
    color: theme.colors.textMuted,
    marginTop: 6,
    fontSize: 11,
  },
  apiDebug: {
    color: theme.colors.textMuted,
    marginTop: 2,
    fontSize: 11,
  },
  messageBar: {
    marginTop: 12,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderWidth: 1,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 10,
  },
  messageBar_success: {
    backgroundColor: theme.colors.card,
    borderColor: theme.colors.tabBorder,
  },
  messageBar_error: {
    backgroundColor: theme.colors.background,
    borderColor: theme.colors.primaryButton,
  },
  messageBar_loading: {
    backgroundColor: theme.colors.background,
    borderColor: theme.colors.tabActive,
  },
  messageBar_neutral: {
    backgroundColor: theme.colors.card,
    borderColor: theme.colors.tabBorder,
  },
  message: {
    color: theme.colors.textSecondary,
    fontWeight: "600",
    flex: 1,
  },
  messageCloseButton: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: theme.colors.tabBorder,
    paddingHorizontal: 10,
    paddingVertical: 4,
    backgroundColor: theme.colors.card,
  },
  messageCloseButtonLabel: {
    color: theme.colors.textPrimary,
    fontSize: 11,
    fontWeight: "700",
  },
  loader: {
    marginTop: 8,
    marginBottom: 8,
  },
  listHeaderWrap: {
    gap: 10,
  },
  segmentWrap: {
    borderRadius: 14,
    padding: 4,
    backgroundColor: theme.colors.card,
    borderWidth: 1,
    borderColor: theme.colors.tabBorder,
    flexDirection: "row",
    gap: 8,
  },
  segmentButton: {
    backgroundColor: "transparent",
    borderWidth: 1,
    borderColor: "transparent",
    borderRadius: 10,
    paddingHorizontal: 10,
    paddingVertical: 8,
    flex: 1,
    alignItems: "center",
  },
  segmentButtonActive: {
    backgroundColor: theme.colors.tabActive,
    borderColor: theme.colors.tabActive,
  },
  segmentLabel: {
    color: theme.colors.textSecondary,
    fontWeight: "700",
  },
  segmentLabelActive: {
    color: theme.colors.tabActiveText,
  },
  filtersWrap: {
    marginTop: 12,
    padding: 10,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: theme.colors.tabBorder,
    backgroundColor: theme.colors.card,
    gap: 8,
  },
  filtersHeading: {
    color: theme.colors.textPrimary,
    fontWeight: "800",
    fontSize: 13,
    textTransform: "uppercase",
    letterSpacing: 0.4,
  },
  filterRowContent: {
    gap: 8,
    paddingRight: 8,
  },
  filterChip: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: theme.colors.tabBorder,
    backgroundColor: theme.colors.background,
    paddingVertical: 6,
    paddingHorizontal: 10,
  },
  filterChipActive: {
    backgroundColor: theme.colors.tabActive,
    borderColor: theme.colors.tabActive,
  },
  filterChipLabel: {
    color: theme.colors.textSecondary,
    fontWeight: "700",
    fontSize: 12,
  },
  filterChipLabelActive: {
    color: theme.colors.tabActiveText,
  },
  filterSummary: {
    color: theme.colors.textMuted,
    fontSize: 12,
    fontWeight: "600",
  },
  listContent: {
    paddingVertical: 14,
    paddingHorizontal: 16,
    gap: 10,
  },
  dataCard: {
    backgroundColor: theme.colors.card,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: theme.colors.tabBorder,
    padding: 14,
    shadowColor: "#000",
    shadowOpacity: 0.12,
    shadowRadius: 10,
    shadowOffset: { width: 0, height: 5 },
    elevation: 3,
  },
  cardHeaderRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: 10,
  },
  categoryChip: {
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 4,
    backgroundColor: theme.colors.background,
    borderWidth: 1,
    borderColor: theme.colors.tabBorder,
  },
  categoryChipText: {
    color: theme.colors.textSecondary,
    fontSize: 11,
    fontWeight: "700",
  },
  title: {
    fontSize: 16,
    fontWeight: "900",
    color: theme.colors.textPrimary,
    flex: 1,
  },
  detail: {
    marginTop: 4,
    color: theme.colors.textSecondary,
    fontSize: 14,
  },
  linkCta: {
    marginTop: 10,
    borderRadius: 10,
    alignSelf: "flex-start",
    paddingVertical: 6,
    paddingHorizontal: 10,
    backgroundColor: theme.colors.background,
    borderWidth: 1,
    borderColor: theme.colors.tabBorder,
  },
  linkCtaLabel: {
    color: theme.colors.link,
    fontWeight: "700",
  },
  empty: {
    textAlign: "center",
    color: theme.colors.textMuted,
    marginTop: 20,
  },
});
