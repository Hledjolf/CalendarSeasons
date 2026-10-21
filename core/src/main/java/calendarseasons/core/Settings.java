package calendarseasons.core;

import java.util.ArrayList;
import java.util.List;

/**
 * An immutable, checked view of {@code config.json}. {@link #checked} fixes out-of-range values and reports
 * each fix, so a bad hand edit never stops the mod.
 */
public record Settings(
    int version,
    String preset,
    long epochYear,
    String yearLabel,
    List<CalendarDef.Month> customMonths,
    List<String> customWeekdays,
    String mode,
    double calendarDaysPerWorldDay,
    long realTimeEpochMillis,
    double utcOffsetHours,
    String dateFormat,
    boolean showWeekday,
    boolean showSeason,
    boolean announceAtDawn,
    boolean announceSeasonChange,
    String announceTemplate) {

    public static final int VERSION = 1;
    public static final String WORLD_DAYS = "world-days";
    public static final String REAL_TIME = "real-time";
    public static final List<String> PRESETS = List.of("harvest", "gregorian", "norse", "japanese", "custom");
    public static final List<String> MODES = List.of(WORLD_DAYS, REAL_TIME);
    public static final long EPOCH_YEAR_MIN = -9999, EPOCH_YEAR_MAX = 9999;
    public static final double RATE_MIN = 0.001, RATE_MAX = 100;
    public static final long REAL_EPOCH_MIN = 0, REAL_EPOCH_MAX = 4_102_444_800_000L; // 2100-01-01
    public static final double UTC_MIN = -12, UTC_MAX = 14;
    public static final long DEFAULT_REAL_EPOCH = 1_767_225_600_000L; // 2026-01-01 00:00 UTC

    public Settings {
        // Null entries (a JSON null in a hand-edited list) become empty ones, which validation then reports.
        customMonths = customMonths == null ? List.of() : customMonths.stream().map(m -> m == null ? new CalendarDef.Month(null, 0, null) : m).toList();
        customWeekdays = customWeekdays == null ? List.of() : customWeekdays.stream().map(w -> w == null ? "" : w).toList();
    }

    /** The defaults, as written to a new {@code config.json}. */
    public static Settings defaults() {
        return new Settings(VERSION, "harvest", 1, "Year {year}",
            List.of(new CalendarDef.Month("Thaw", 45, "Spring"), new CalendarDef.Month("Sowing", 45, "Spring")),
            List.of("Moonday", "Tideday", "Windsday", "Thunderday", "Fireday", "Starday", "Sunday"),
            WORLD_DAYS, 1.0, DEFAULT_REAL_EPOCH, 0.0,
            Formatter.FORMATS.get(0), true, true, false, true, "Dawn breaks: {date}");
    }

    /** A checked copy plus one plain-words line per value that had to be fixed. */
    public record Checked(Settings settings, List<String> fixes) {}

    public Checked checked() {
        Settings d = defaults();
        List<String> fixes = new ArrayList<>();
        String p = preset == null ? null : preset.strip().toLowerCase(java.util.Locale.ROOT);
        if (!PRESETS.contains(p)) {
            fixes.add("Calendar.Preset \"" + preset + "\" is not one of " + PRESETS + "; using harvest");
            p = d.preset;
        }
        String m = mode == null ? null : mode.strip().toLowerCase(java.util.Locale.ROOT);
        if (!MODES.contains(m)) {
            fixes.add("Clock.Mode \"" + mode + "\" is not one of " + MODES + "; using world-days");
            m = d.mode;
        }
        long ey = clamp(epochYear, EPOCH_YEAR_MIN, EPOCH_YEAR_MAX, "Calendar.EpochYear", fixes);
        double rate = calendarDaysPerWorldDay;
        if (!(rate >= RATE_MIN && rate <= RATE_MAX)) {
            double r = Double.isNaN(rate) ? 1.0 : Math.max(RATE_MIN, Math.min(RATE_MAX, rate));
            fixes.add("Clock.CalendarDaysPerWorldDay " + rate + " is outside " + RATE_MIN + " to " + (int) RATE_MAX + "; using " + r);
            rate = r;
        }
        long ep = clamp(realTimeEpochMillis, REAL_EPOCH_MIN, REAL_EPOCH_MAX, "Clock.RealTimeEpochMillis", fixes);
        double utc = utcOffsetHours;
        if (!(utc >= UTC_MIN && utc <= UTC_MAX)) {
            double u = Double.isNaN(utc) ? 0 : Math.max(UTC_MIN, Math.min(UTC_MAX, utc));
            fixes.add("Clock.UtcOffsetHours " + utc + " is outside -12 to 14; using " + u);
            utc = u;
        }
        String fmt = dateFormat == null || dateFormat.isBlank() ? d.dateFormat : dateFormat;
        if (fmt != dateFormat) fixes.add("Display.DateFormat is empty; using " + fmt);
        String label = yearLabel == null ? d.yearLabel : yearLabel;
        String tpl = announceTemplate == null ? d.announceTemplate : announceTemplate;
        int v = version <= 0 ? VERSION : version;
        return new Checked(new Settings(v, p, ey, label, customMonths, customWeekdays, m, rate, ep, utc, fmt,
            showWeekday, showSeason, announceAtDawn, announceSeasonChange, tpl), List.copyOf(fixes));
    }

    private static long clamp(long v, long min, long max, String key, List<String> fixes) {
        if (v >= min && v <= max) return v;
        long c = Math.max(min, Math.min(max, v));
        fixes.add(key + " " + v + " is outside " + min + " to " + max + "; using " + c);
        return c;
    }

    public boolean realTime() {
        return REAL_TIME.equals(mode);
    }
}
