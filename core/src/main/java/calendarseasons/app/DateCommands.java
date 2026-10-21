package calendarseasons.app;

import calendarseasons.core.CalendarDef;
import calendarseasons.core.DateMath;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The text of {@code /date}: {@code /date}, {@code /date set <day> <month> [year]}, {@code /date months},
 * {@code /date season}. The adapter passes in the arguments, whether the sender may set the date, and the
 * clocks.
 */
public final class DateCommands {
    public static final String NAME = "date";
    public static final String PERMISSION = "calendarseasons.set";

    private final CalendarService service;

    public DateCommands(CalendarService service) {
        this.service = service;
    }

    /** The command's arguments after the command name, split on whitespace. */
    public static List<String> split(String input) {
        List<String> out = new ArrayList<>();
        if (input == null) return out;
        String s = input.strip();
        if (s.startsWith("/")) s = s.substring(1);
        for (String t : s.split("\\s+")) if (!t.isEmpty()) out.add(t);
        if (!out.isEmpty() && out.get(0).equalsIgnoreCase(NAME)) out.remove(0);
        return out;
    }

    public static List<String> usage() {
        return List.of(
            "/date  shows today's date",
            "/date season  shows the season and when the next one begins",
            "/date months  lists the months of the year",
            "/date set <day> <month> [year]  changes today's date (operators, or " + PERMISSION + ")");
    }

    public List<String> run(List<String> args, boolean maySet, long worldDawns, long nowMillis) {
        if (args.isEmpty()) return List.of(service.format(service.today(worldDawns, nowMillis)));
        String sub = args.get(0).toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "help" -> usage();
            case "months" -> months(worldDawns, nowMillis);
            case "season" -> season(worldDawns, nowMillis);
            case "set" -> set(args.subList(1, args.size()), maySet, worldDawns, nowMillis);
            default -> {
                List<String> l = new ArrayList<>();
                l.add("Unknown /date option \"" + args.get(0) + "\".");
                l.addAll(usage());
                yield l;
            }
        };
    }

    private List<String> months(long worldDawns, long nowMillis) {
        CalendarDef cal = service.calendar();
        DateMath.Date today = service.today(worldDawns, nowMillis);
        List<String> out = new ArrayList<>();
        out.add("The " + cal.id() + " calendar: " + cal.months().size() + " months, " + cal.yearLength() + " days a year.");
        for (int k = 0; k < cal.months().size(); k++) {
            CalendarDef.Month m = cal.months().get(k);
            out.add(String.format("  %d. %s, %d days, %s%s", k + 1, m.name(), m.days(), m.season(), k == today.month() ? "  (now)" : ""));
        }
        out.add("Weekdays: " + String.join(", ", cal.weekdays()));
        return out;
    }

    private List<String> season(long worldDawns, long nowMillis) {
        CalendarDef cal = service.calendar();
        DateMath.Date d = service.today(worldDawns, nowMillis);
        long n = service.dayNumber(worldDawns, nowMillis);
        long until = DateMath.daysUntilNextSeason(cal, n, service.settings().epochYear());
        if (until < 0) return List.of("It is " + d.season() + ", the only season of this calendar.");
        String next = DateMath.dateOf(cal, n + until, service.settings().epochYear()).season();
        return List.of("It is " + d.season() + ". " + next + " begins in " + until + (until == 1 ? " day." : " days."));
    }

    private List<String> set(List<String> a, boolean maySet, long worldDawns, long nowMillis) {
        if (!maySet) return List.of("Only operators, or players with the " + PERMISSION + " permission, can change the date.");
        if (a.size() < 2 || a.size() > 3) return List.of("Usage: /date set <day> <month> [year]   (month: a name or a number)");
        CalendarDef cal = service.calendar();
        int day;
        try {
            day = Integer.parseInt(a.get(0));
        } catch (NumberFormatException e) {
            return List.of("\"" + a.get(0) + "\" is not a day number.");
        }
        int month = cal.monthIndex(a.get(1));
        if (month < 0) {
            try {
                month = Integer.parseInt(a.get(1)) - 1;
            } catch (NumberFormatException e) {
                return List.of("There is no month \"" + a.get(1) + "\". Try /date months.");
            }
        }
        long year = service.today(worldDawns, nowMillis).year();
        if (a.size() == 3) {
            try {
                year = Long.parseLong(a.get(2));
            } catch (NumberFormatException e) {
                return List.of("\"" + a.get(2) + "\" is not a year number.");
            }
        }
        try {
            DateMath.Date d = service.setDate(year, month, day, worldDawns, nowMillis);
            return List.of("The date is now " + service.format(d) + ".");
        } catch (IllegalArgumentException | ArithmeticException e) {
            return List.of("Can't set that date: " + e.getMessage() + ".");
        }
    }
}
