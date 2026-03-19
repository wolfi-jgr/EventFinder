package com.example.eventfinder.scraper.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing various date/time formats commonly found on event websites.
 * Handles German locale dates, day names, and flexible formats.
 */
public class DateParser {
    private static final Logger logger = LoggerFactory.getLogger(DateParser.class);
    
    private static final Map<String, Integer> GERMAN_MONTHS = Map.ofEntries(
        Map.entry("januar", 1),
        Map.entry("jaenner", 1),
        Map.entry("februar", 2),
        Map.entry("feber", 2),
        Map.entry("maerz", 3),
        Map.entry("marz", 3),
        Map.entry("april", 4),
        Map.entry("mai", 5),
        Map.entry("juni", 6),
        Map.entry("juli", 7),
        Map.entry("august", 8),
        Map.entry("september", 9),
        Map.entry("oktober", 10),
        Map.entry("november", 11),
        Map.entry("dezember", 12)
    );
    
    private static final Map<String, Integer> GERMAN_DAYS = Map.ofEntries(
        Map.entry("montag", 1),
        Map.entry("dienstag", 2),
        Map.entry("mittwoch", 3),
        Map.entry("donnerstag", 4),
        Map.entry("freitag", 5),
        Map.entry("samstag", 6),
        Map.entry("sonntag", 7),
        Map.entry("mo", 1),
        Map.entry("di", 2),
        Map.entry("mi", 3),
        Map.entry("do", 4),
        Map.entry("fr", 5),
        Map.entry("sa", 6),
        Map.entry("so", 7)
    );
    
    /**
     * Parse date/time string using format and locale from rule
     */
    public static LocalDateTime parseDateTime(String dateStr, String format, String locale) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        String cleaned = dateStr.replace('\u00A0', ' ').trim();
        cleaned = cleaned.replace('–', '-').replace('—', '-');
        cleaned = cleaned.replace("Jänner", "Januar").replace("Feber", "Februar");
        cleaned = cleaned.replace("MÃ¤rz", "März").replace("M├ñrz", "März");
        Locale parsedLocale = locale != null ? Locale.forLanguageTag(locale) : Locale.getDefault();

        try {
            Matcher timeMatcher = Pattern.compile("(\\d{1,2}:\\d{2})").matcher(cleaned);
            String timePart = timeMatcher.find() ? timeMatcher.group(1) : null;

            LocalDateTime looseSlashDate = parseLooseSlashDate(cleaned, timePart);
            if (looseSlashDate != null) {
                return looseSlashDate;
            }

            Matcher shortDateAny = Pattern.compile("(\\d{1,2})/(\\d{1,2})").matcher(cleaned);
            if (shortDateAny.find()) {
                int day = Integer.parseInt(shortDateAny.group(1));
                int month = Integer.parseInt(shortDateAny.group(2));
                int year = LocalDate.now().getYear();
                LocalDate date = LocalDate.of(year, month, day);
                LocalTime time = timePart != null ? LocalTime.parse(timePart) : LocalTime.MIDNIGHT;
                return LocalDateTime.of(date, time);
            }

            LocalDateTime dayMonthAlpha = parseDayMonthAlpha(cleaned, timePart, parsedLocale);
            if (dayMonthAlpha != null) {
                return dayMonthAlpha;
            }

            // Only use weekday-name fallback when there is no explicit calendar date in the text.
            LocalDateTime dayNameDate = parseGermanDayName(cleaned, timePart);
            if (dayNameDate != null) {
                return dayNameDate;
            }

            Matcher namedDateAny = Pattern.compile("(\\d{1,2})\\.\\s*([\\p{L}]+)(?:\\s*(\\d{4}))?").matcher(cleaned);
            String day = null;
            String monthName = null;
            String year = null;
            while (namedDateAny.find()) {
                day = namedDateAny.group(1);
                monthName = namedDateAny.group(2);
                year = namedDateAny.group(3);
            }

            if (day != null && monthName != null) {
                String resolvedYear = year != null ? year : String.valueOf(LocalDate.now().getYear());
                String normalizedMonth = normalizeMonthName(monthName);
                Integer month = GERMAN_MONTHS.get(normalizedMonth);
                if (month != null) {
                    LocalDate date = LocalDate.of(Integer.parseInt(resolvedYear), month, Integer.parseInt(day));
                    LocalTime time = timePart != null ? LocalTime.parse(timePart) : LocalTime.MIDNIGHT;
                    return LocalDateTime.of(date, time);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse date '{}' with format '{}': {}", cleaned, format, e.getMessage());
        }

        try {
            if (format != null && !format.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withLocale(parsedLocale);
                return LocalDateTime.parse(cleaned, formatter);
            }
            // Try ISO format as fallback
            return LocalDateTime.parse(cleaned);
        } catch (DateTimeParseException e) {
            // Fall through to flexible parsing below.
        }

        try {
            String cleanedForDate = cleaned.replaceAll("^[^0-9]+", "");
            if (cleanedForDate.isEmpty()) {
                cleanedForDate = cleaned;
            }

            Matcher leadingShortDate = Pattern.compile("^(\\d{1,2})/(\\d{1,2})").matcher(cleanedForDate);
            if (leadingShortDate.find()) {
                String resolvedYear = String.valueOf(LocalDate.now().getYear());
                String dateToParse = leadingShortDate.group(1) + "/" + leadingShortDate.group(2) + "/" + resolvedYear;
                LocalDate date = LocalDate.parse(dateToParse, DateTimeFormatter.ofPattern("d/M/yyyy"));
                return LocalDateTime.of(date, LocalTime.MIDNIGHT);
            }

            String timePart = null;
            Matcher timeMatcher = Pattern.compile("(\\d{1,2}:\\d{2})").matcher(cleanedForDate);
            if (timeMatcher.find()) {
                timePart = timeMatcher.group(1);
            }

            String datePart = null;
            Matcher numericDateMatcher = Pattern.compile("(\\d{1,2}[./]\\d{1,2}[./]\\d{2,4})").matcher(cleanedForDate);
            while (numericDateMatcher.find()) {
                datePart = numericDateMatcher.group(1);
            }

            if (datePart != null) {
                String normalizedDate = datePart.replace('/', '.');
                String[] parts = normalizedDate.split("\\.");
                if (parts.length == 3) {
                    String year = parts[2];
                    if (year.length() == 2) {
                        year = "20" + year;
                    }
                    String dateToParse = parts[0] + "." + parts[1] + "." + year;
                    LocalDate date = LocalDate.parse(dateToParse, DateTimeFormatter.ofPattern("d.M.yyyy"));
                    LocalTime time = timePart != null ? LocalTime.parse(timePart) : LocalTime.MIDNIGHT;
                    return LocalDateTime.of(date, time);
                }
            }

            Matcher namedDateMatcher = Pattern.compile("(\\d{1,2})\\.\\s*([\\p{L}]+)(?:\\s*(\\d{4}))?").matcher(cleanedForDate);
            String day = null;
            String monthName = null;
            String year = null;
            while (namedDateMatcher.find()) {
                day = namedDateMatcher.group(1);
                monthName = namedDateMatcher.group(2);
                year = namedDateMatcher.group(3);
            }

            if (day != null && monthName != null) {
                String resolvedYear = year != null ? year : String.valueOf(LocalDate.now().getYear());
                String normalizedMonth = normalizeMonthName(monthName);
                Integer month = GERMAN_MONTHS.get(normalizedMonth);
                LocalDate date;
                if (month != null) {
                    date = LocalDate.of(Integer.parseInt(resolvedYear), month, Integer.parseInt(day));
                } else {
                    String dateToParse = day + ". " + monthName + " " + resolvedYear;
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", parsedLocale);
                    date = LocalDate.parse(dateToParse, formatter);
                }
                LocalTime time = timePart != null ? LocalTime.parse(timePart) : LocalTime.MIDNIGHT;
                return LocalDateTime.of(date, time);
            }

            Matcher shortDateMatcher = Pattern.compile("(\\d{1,2})/(\\d{1,2})").matcher(cleanedForDate);
            if (shortDateMatcher.find()) {
                String dayPart = shortDateMatcher.group(1);
                String monthPart = shortDateMatcher.group(2);
                String resolvedYear = String.valueOf(LocalDate.now().getYear());
                String dateToParse = dayPart + "/" + monthPart + "/" + resolvedYear;
                LocalDate date = LocalDate.parse(dateToParse, DateTimeFormatter.ofPattern("d/M/yyyy"));
                LocalTime time = timePart != null ? LocalTime.parse(timePart) : LocalTime.MIDNIGHT;
                return LocalDateTime.of(date, time);
            }

            LocalDateTime dayMonthAlpha = parseDayMonthAlpha(cleanedForDate, timePart, parsedLocale);
            if (dayMonthAlpha != null) {
                return dayMonthAlpha;
            }
        } catch (Exception e) {
            logger.warn("Failed to parse date '{}' with format '{}': {}", cleaned, format, e.getMessage());
        }

        logger.warn("Failed to parse date '{}' with format '{}'", cleaned, format);
        return null;
    }
    
    /**
     * Parse German day name and return LocalDateTime for next occurrence
     * e.g., "Montag, 19:30 Uhr" -> next Monday at 19:30
     */
    public static LocalDateTime parseGermanDayName(String text, String timePart) {
        if (text == null) return null;

        if (containsExplicitCalendarDate(text)) {
            return null;
        }
        
        String lower = text.toLowerCase()
            .replace(",", "")
            .replace(".", "")
            .replace(" uhr", "")
            .trim();
        
        // Find day name in text
        for (Map.Entry<String, Integer> entry : GERMAN_DAYS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                int targetDayOfWeek = entry.getValue();
                LocalDate today = LocalDate.now();
                LocalDate targetDate = today;
                
                // Find next occurrence of this day (including today if it matches)
                while (targetDate.getDayOfWeek().getValue() != targetDayOfWeek) {
                    targetDate = targetDate.plusDays(1);
                    // Safety limit - don't search beyond 7 days
                    if (targetDate.isAfter(today.plusDays(7))) {
                        return null;
                    }
                }
                
                // Parse time or default to 19:00
                LocalTime time = LocalTime.of(19, 0);
                if (timePart != null) {
                    try {
                        time = LocalTime.parse(timePart);
                    } catch (Exception e) {
                        // Keep default time
                    }
                }
                
                return LocalDateTime.of(targetDate, time);
            }
        }
        
        return null;
    }

    private static boolean containsExplicitCalendarDate(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        return Pattern.compile("\\d{1,2}[./]\\d{1,2}(?:[./]\\d{2,4})?").matcher(text).find()
            || Pattern.compile("\\d{1,2}\\.\\s*[\\p{L}]+(?:\\s*\\d{4})?").matcher(text).find();
    }
    
    /**
     * Parse date from URL patterns like /2812-eventname
     */
    public static LocalDateTime parseDateFromUrl(String url) {
        if (url == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("/(\\d{2})(\\d{2})-").matcher(url);
        if (matcher.find()) {
            int day = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int year = LocalDate.now().getYear();
            return LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.MIDNIGHT);
        }
        return null;
    }
    
    private static String normalizeMonthName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        normalized = normalized
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss");
        return normalized.replaceAll("[^a-z]", "");
    }
    
    private static LocalDateTime parseLooseSlashDate(String value, String timePart) {
        int year = LocalDate.now().getYear();
        for (int i = 0; i + 4 < value.length(); i++) {
            char d1 = value.charAt(i);
            char d2 = value.charAt(i + 1);
            char sep = value.charAt(i + 2);
            char m1 = value.charAt(i + 3);
            char m2 = value.charAt(i + 4);

            if (Character.isDigit(d1) && Character.isDigit(d2) && sep == '/' && Character.isDigit(m1) && Character.isDigit(m2)) {
                int day = Integer.parseInt("" + d1 + d2);
                int month = Integer.parseInt("" + m1 + m2);
                LocalDate date = LocalDate.of(year, month, day);
                LocalTime time = timePart != null ? LocalTime.parse(timePart) : LocalTime.MIDNIGHT;
                return LocalDateTime.of(date, time);
            }
        }
        return null;
    }

    private static LocalDateTime parseDayMonthAlpha(String value, String timePart, Locale parsedLocale) {
        if (value == null || value.isBlank()) {
            return null;
        }

        Matcher matcher = Pattern.compile("(\\d{1,2})\\s*([\\p{L}]{3,})").matcher(value);
        if (!matcher.find()) {
            return null;
        }

        int day = Integer.parseInt(matcher.group(1));
        String monthToken = matcher.group(2);
        Integer month = parseMonthToken(monthToken, parsedLocale);
        if (month == null) {
            return null;
        }

        int year = LocalDate.now().getYear();
        LocalDate date = LocalDate.of(year, month, day);
        LocalTime time = timePart != null ? LocalTime.parse(timePart) : LocalTime.MIDNIGHT;
        return LocalDateTime.of(date, time);
    }

    private static Integer parseMonthToken(String token, Locale parsedLocale) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String normalized = normalizeMonthName(token);
        Integer germanMonth = GERMAN_MONTHS.get(normalized);
        if (germanMonth != null) {
            return germanMonth;
        }

        Map<String, Integer> shortMonths = Map.ofEntries(
            Map.entry("jan", 1),
            Map.entry("feb", 2),
            Map.entry("mar", 3),
            Map.entry("apr", 4),
            Map.entry("may", 5),
            Map.entry("jun", 6),
            Map.entry("jul", 7),
            Map.entry("aug", 8),
            Map.entry("sep", 9),
            Map.entry("oct", 10),
            Map.entry("nov", 11),
            Map.entry("dec", 12),
            Map.entry("mrz", 3),
            Map.entry("mae", 3),
            Map.entry("okt", 10),
            Map.entry("dez", 12)
        );
        Integer shortMonth = shortMonths.get(normalized.length() >= 3 ? normalized.substring(0, 3) : normalized);
        if (shortMonth != null) {
            return shortMonth;
        }

        try {
            return DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
                .parse(token.trim())
                .get(java.time.temporal.ChronoField.MONTH_OF_YEAR);
        } catch (Exception ignored) {
        }

        try {
            return DateTimeFormatter.ofPattern("MMM", parsedLocale)
                .parse(token.trim())
                .get(java.time.temporal.ChronoField.MONTH_OF_YEAR);
        } catch (Exception ignored) {
        }

        return null;
    }
}
