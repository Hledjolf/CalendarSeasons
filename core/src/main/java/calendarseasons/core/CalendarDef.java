package calendarseasons.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One calendar: months (name, days, season), weekday names, and two presentation details. Immutable. A year
 * is the sum of the months' days. Build one with {@link #of} (which validates) or from JSON with
 * {@link #fromJson}.
 *
 * @param id               the preset id, or {@code custom}
 * @param months           at least one month
 * @param weekdays         at least one weekday name
 * @param englishOrdinals  whether {@code {ordinal}} gives st, nd, rd and th (otherwise it is empty)
 * @param seasonTemplate   the season-change line, with {@code {season}}
 */
public record CalendarDef(String id, List<Month> months, List<String> weekdays, boolean englishOrdinals, String seasonTemplate) {
    public static final String DEFAULT_SEASON_TEMPLATE = "{season} has come.";
    public static final int MAX_MONTHS = 100;
    public static final int MAX_MONTH_DAYS = 10_000;

    public record Month(String name, int days, String season) {}

    public CalendarDef {
        months = List.copyOf(months);
        weekdays = List.copyOf(weekdays);
    }

    public int yearLength() {
        int n = 0;
        for (Month m : months) n += m.days();
        return n;
    }

    /** The problems that make this calendar unusable, in plain words; empty when it's fine. */
    public static List<String> problems(List<Month> months, List<String> weekdays) {
        List<String> out = new ArrayList<>();
        if (months == null || months.isEmpty()) out.add("there are no months");
        else {
            if (months.size() > MAX_MONTHS) out.add("there are more than " + MAX_MONTHS + " months");
            for (int k = 0; k < months.size(); k++) {
                Month m = months.get(k);
                String which = "month " + (k + 1);
                if (m == null) {
                    out.add(which + " is empty");
                    continue;
                }
                if (m.name() == null || m.name().isBlank()) out.add(which + " has no Name");
                if (m.days() < 1 || m.days() > MAX_MONTH_DAYS) out.add(which + " has Days " + m.days() + " (must be 1 to " + MAX_MONTH_DAYS + ")");
                if (m.season() == null || m.season().isBlank()) out.add(which + " has no Season");
            }
        }
        if (weekdays == null || weekdays.isEmpty()) out.add("there are no weekdays");
        else for (int k = 0; k < weekdays.size(); k++) {
            if (weekdays.get(k) == null || weekdays.get(k).isBlank()) out.add("weekday " + (k + 1) + " is empty");
        }
        return out;
    }

    /** A validated calendar. Throws {@link IllegalArgumentException} listing every problem. */
    public static CalendarDef of(String id, List<Month> months, List<String> weekdays, boolean englishOrdinals, String seasonTemplate) {
        List<String> p = problems(months, weekdays);
        if (!p.isEmpty()) throw new IllegalArgumentException(String.join("; ", p));
        return new CalendarDef(id, months, weekdays, englishOrdinals,
            seasonTemplate == null || seasonTemplate.isBlank() ? DEFAULT_SEASON_TEMPLATE : seasonTemplate);
    }

    /**
     * A calendar from a preset file: {@code Id}, {@code Months} ({@code Name}, {@code Days}, {@code Season}),
     * {@code Weekdays}, and optionally {@code Ordinals} ({@code english} or {@code none}) and
     * {@code SeasonAnnouncement}.
     */
    public static CalendarDef fromJson(Map<String, Object> o) {
        String id = o.get("Id") instanceof String s ? s : "unnamed";
        List<Month> months = new ArrayList<>();
        if (o.get("Months") instanceof List<?> l) {
            for (Object e : l) {
                if (e instanceof Map<?, ?> m) {
                    Object days = m.get("Days");
                    months.add(new Month(m.get("Name") instanceof String n ? n : null,
                        days instanceof Number d && d.doubleValue() == Math.rint(d.doubleValue()) ? (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, d.longValue())) : 0,
                        m.get("Season") instanceof String s ? s : null));
                } else months.add(null);
            }
        }
        List<String> weekdays = new ArrayList<>();
        if (o.get("Weekdays") instanceof List<?> l) for (Object e : l) weekdays.add(e instanceof String s ? s : null);
        boolean english = !"none".equals(o.get("Ordinals"));
        String season = o.get("SeasonAnnouncement") instanceof String s ? s : null;
        return of(id, months, weekdays, english, season);
    }

    /** The seasons in order of first appearance, each once. */
    public List<String> seasons() {
        List<String> out = new ArrayList<>();
        for (Month m : months) if (!out.contains(m.season())) out.add(m.season());
        return out;
    }

    /** The month index for a name, ignoring case, or -1. */
    public int monthIndex(String name) {
        for (int k = 0; k < months.size(); k++) if (months.get(k).name().equalsIgnoreCase(name.strip())) return k;
        return -1;
    }
}
