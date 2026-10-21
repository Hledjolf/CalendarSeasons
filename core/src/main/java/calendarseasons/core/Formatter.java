package calendarseasons.core;

import java.util.List;

/**
 * Writes a date with a {@code DateFormat} template. Tokens: {@code {monthName} {month} {month2} {day} {day2}
 * {dayOfYear} {year} {yearLabel} {weekday} {season} {ordinal}}. Unknown tokens stay as written.
 *
 * <p>The weekday and season switches: when on and the format doesn't already contain the token, the weekday
 * is put in front ("Moonday, ...") and the season at the end ("..., Spring"). When off, the token gives an
 * empty string and the separators around it are tidied away.
 */
public final class Formatter {
    /** The twelve formats offered in the settings dropdown, in order. Any other template works too. */
    public static final List<String> FORMATS = List.of(
        "{monthName} {day}, {yearLabel}",
        "{day} {monthName} {yearLabel}",
        "{weekday}, {monthName} {day}, {yearLabel}",
        "{weekday} {day} {monthName}",
        "{day}/{month}/{year}",
        "{month}/{day}/{year}",
        "{year}-{month2}-{day2}",
        "{day}. {monthName} {year}",
        "Day {dayOfYear} of {yearLabel}",
        "{monthName} {day}",
        "{season}, {monthName} {day}",
        "{day}{ordinal} of {monthName}, {yearLabel}");

    private Formatter() {}

    /** The full date line, with the weekday and season switches applied. */
    public static String format(DateMath.Date d, CalendarDef cal, String template, String yearLabel, boolean showWeekday, boolean showSeason) {
        String body = fill(template, d, cal, yearLabel, showWeekday, showSeason);
        if (showWeekday && !template.contains("{weekday}")) body = d.weekdayName() + ", " + body;
        if (showSeason && !template.contains("{season}")) body = body + ", " + d.season();
        return tidy(body);
    }

    /** The year label with {@code {year}} filled in. */
    public static String yearLabel(String label, long year) {
        return (label == null ? "{year}" : label).replace("{year}", Long.toString(year));
    }

    static String fill(String t, DateMath.Date d, CalendarDef cal, String yearLabel, boolean showWeekday, boolean showSeason) {
        return t
            .replace("{monthName}", d.monthName())
            .replace("{month2}", two(d.month() + 1))
            .replace("{month}", Integer.toString(d.month() + 1))
            .replace("{day2}", two(d.day()))
            .replace("{dayOfYear}", Integer.toString(d.dayOfYear()))
            .replace("{day}", Integer.toString(d.day()))
            .replace("{yearLabel}", yearLabel(yearLabel, d.year()))
            .replace("{year}", Long.toString(d.year()))
            .replace("{weekday}", showWeekday ? d.weekdayName() : "")
            .replace("{season}", showSeason ? d.season() : "")
            .replace("{ordinal}", cal.englishOrdinals() ? ordinal(d.day()) : "");
    }

    static String two(int n) {
        return n < 10 ? "0" + n : Integer.toString(n);
    }

    /** English ordinal suffix: 1st, 2nd, 3rd, 4th, 11th, 12th, 13th, 21st, 111th. */
    public static String ordinal(long n) {
        long a = Math.abs(n);
        if (a % 100 >= 11 && a % 100 <= 13) return "th";
        return switch ((int) (a % 10)) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    /** Removes separators left dangling by an empty token: leading or trailing ", ", ", ," and double spaces. */
    static String tidy(String s) {
        String out = s;
        String before;
        do {
            before = out;
            out = out.replace(" ,", ",").replace(",,", ",").replace("  ", " ");
        } while (!out.equals(before));
        out = out.strip();
        while (out.startsWith(",")) out = out.substring(1).strip();
        while (out.endsWith(",")) out = out.substring(0, out.length() - 1).strip();
        return out;
    }
}
