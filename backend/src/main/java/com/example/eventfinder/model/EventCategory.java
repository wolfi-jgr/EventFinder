package com.example.eventfinder.model;

/**
 * Enum for classifying events into standardized categories/genres.
 * Used for filtering, display, and recommendation logic.
 */
public enum EventCategory {

    /** Live music performances – concerts, gigs, bands, DJ sets */
    CONCERT,

    /** Club nights, DJ events, dancing */
    PARTY,

    /** Theater, opera, cabaret, spoken word */
    THEATER,

    /** Art exhibitions, gallery openings, vernissages, installations */
    EXHIBITION,

    /** Sports events, tournaments, fitness classes */
    SPORTS,

    /** Food markets, culinary events, tastings, cooking classes */
    FOOD,

    /** Conferences, networking, meetups, business talks */
    BUSINESS,

    /** Workshops, hands-on courses, creative classes */
    WORKSHOP,

    /** Family-oriented, children, youth events */
    FAMILY,

    /** Generic event that doesn't fit another category */
    EVENT,

    /** Anything else not clearly classifiable */
    OTHER;

    /** German display label for UI */
    public String getLabel() {
        return switch (this) {
            case CONCERT    -> "Konzert";
            case PARTY      -> "Party";
            case THEATER    -> "Theater";
            case EXHIBITION -> "Ausstellung";
            case SPORTS     -> "Sport";
            case FOOD       -> "Food";
            case BUSINESS   -> "Business";
            case WORKSHOP   -> "Workshop";
            case FAMILY     -> "Familie";
            case EVENT      -> "Veranstaltung";
            case OTHER      -> "Sonstiges";
        };
    }

    /** Emoji icon for display */
    public String getIcon() {
        return switch (this) {
            case CONCERT    -> "🎵";
            case PARTY      -> "🎉";
            case THEATER    -> "🎭";
            case EXHIBITION -> "🖼️";
            case SPORTS     -> "⚽";
            case FOOD       -> "🍕";
            case BUSINESS   -> "💼";
            case WORKSHOP   -> "🔧";
            case FAMILY     -> "👨‍👩‍👧";
            case EVENT      -> "📅";
            case OTHER      -> "✨";
        };
    }
}
