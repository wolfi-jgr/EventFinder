package com.example.eventfinder.scraper.utils;

import com.example.eventfinder.model.EventCategory;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Keyword-based event category classifier.
 * Scores each category by how many keywords match in the event's title and description.
 * Returns the highest-scoring category, falling back to EVENT when nothing matches.
 */
public class CategoryClassifier {

    /** Ordered keyword lists per category (German + English, lower-case). */
    private static final Map<EventCategory, List<String>> KEYWORDS = new EnumMap<>(EventCategory.class);

    static {
        KEYWORDS.put(EventCategory.THEATER, Arrays.asList(
                "theater", "theatre", "oper", "opera", "schauspiel", "bühnenstück",
                "kabarett", "cabaret", "bühne", "puppet", "puppen", "marionette",
                "dramatik", "komödie", "tragödie", "comedy show", "stand-up",
                "spoken word", "lesung", "literaturabend", "lecture performance"
        ));

        KEYWORDS.put(EventCategory.EXHIBITION, Arrays.asList(
                "ausstellung", "exhibition", "vernissage", "galerie", "gallery",
                "kunst", "art show", "installation", "museum", "finissage",
                "foto", "photography", "skulptur", "sculpture", "malerei",
                "open studio", "atelierausstellung"
        ));

        KEYWORDS.put(EventCategory.SPORTS, Arrays.asList(
                "sport", "turnier", "tournament", "marathon", "lauf", "run",
                "schwimm", "swim", "boxen", "boxing", "kampfsport", "fight night",
                "yoga", "pilates", "fitness", "workout", "training", "match",
                "spiel", "liga", "wettbewerb", "competition", "meisterschaft",
                "championship", "tennis", "fußball", "basketball", "volleyball"
        ));

        KEYWORDS.put(EventCategory.FOOD, Arrays.asList(
                "food", "essen", "kulinarisch", "culinary", "kochkurs",
                "cooking class", "tasting", "verkostung", "weinprobe",
                "wine", "wein", "bier", "beer", "brunch", "streetfood",
                "street food", "markt", " market", "gourmet", "dinner",
                "abendessen", "supper", "brauerei", "destillerie", "mixology"
        ));

        KEYWORDS.put(EventCategory.BUSINESS, Arrays.asList(
                "konferenz", "conference", "meetup", "networking", "panel",
                "summit", "congress", "kongress", "startup", "pitch",
                "business talk", "industry talk", "vortrag", "lecture",
                "präsentation", "presentation", "roundtable"
        ));

        KEYWORDS.put(EventCategory.WORKSHOP, Arrays.asList(
                "workshop", "kurs", "seminar", "class", "hands-on",
                "mitmachen", "do it yourself", "diy", "lernen", "learn",
                "kochkurs", "tanzworkshop", "malworkshop", "atelier",
                "weiterbildung", "creativ", "kreativ", "bastel", "craft"
        ));

        KEYWORDS.put(EventCategory.FAMILY, Arrays.asList(
                "kinder", "kids", "family", "familien", "jugend", "youth",
                "schule", "school", "kindergarten", "eltern", "parents",
                "märchen", "fairytale", "kinderprogramm", "für kinder"
        ));

        KEYWORDS.put(EventCategory.PARTY, Arrays.asList(
                "party", "night", "clubnight", "club night", "after party",
                "afterparty", "silvester", "halloween", "rave", "dancehall",
                "dance night", "floor", "closing", "opening party",
                "birthday bash", "release party", "boat party", "beach party",
                "closing event", "warm-up party"
        ));

        KEYWORDS.put(EventCategory.CONCERT, Arrays.asList(
                "konzert", "concert", "live musik", "live music", "liveband",
                "live-band", "gig", "dj set", "dj-set", "studiokonzert",
                "open air", "festival", "soundsystem", "music night",
                "musiknacht", "elektronische musik", "electronic music",
                "techno", "jazz", "blues", "rock", "folk", "klassik",
                "classical", "orchestral", "acoustic", "akustik",
                "singer songwriter", "singer-songwriter", "band night",
                "tribute band", "tribute concert", "release concert"
        ));
    }

    /**
     * Classify an event into a category based on title and optional description.
     *
     * @param title       event title (may be null)
     * @param description event description (may be null)
     * @return best-matching {@link EventCategory}, never null
     */
    public static EventCategory classify(String title, String description) {
        String combined = buildSearchText(title, description);
        if (combined.isEmpty()) {
            return EventCategory.EVENT;
        }

        EventCategory best = null;
        int bestScore = 0;

        for (Map.Entry<EventCategory, List<String>> entry : KEYWORDS.entrySet()) {
            int score = score(combined, entry.getValue());
            if (score > bestScore) {
                bestScore = score;
                best = entry.getKey();
            }
        }

        return best != null ? best : EventCategory.EVENT;
    }

    /**
     * Classify and return the enum name as a String (convenience for DB storage).
     */
    public static String classifyAsString(String title, String description) {
        return classify(title, description).name();
    }

    /**
     * Attempt to parse a string value into an EventCategory.
     * Returns null if the value does not match any known category.
     */
    public static EventCategory fromString(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return EventCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String buildSearchText(String title, String description) {
        StringBuilder sb = new StringBuilder();
        if (title != null) sb.append(title.toLowerCase());
        if (description != null) sb.append(' ').append(description.toLowerCase());
        return sb.toString();
    }

    private static int score(String text, List<String> keywords) {
        int hits = 0;
        for (String kw : keywords) {
            if (text.contains(kw)) {
                hits++;
            }
        }
        return hits;
    }
}
