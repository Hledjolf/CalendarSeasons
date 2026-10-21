package calendarseasons.core;

/**
 * Calendar day numbers and dates. Calendar day 0 is the first day of {@code EpochYear}; negative day
 * numbers are the years before it. Pure functions, no state.
 */
public final class DateMath {
    public static final long MILLIS_PER_DAY = 86_400_000L;
    public static final long MILLIS_PER_HOUR = 3_600_000L;

    private DateMath() {}

    /**
     * One date.
     *
     * @param year       the year number (EpochYear plus whole years since day 0; may be negative)
     * @param month      the month index, 0-based
     * @param day        the day of the month, 1-based
     * @param dayOfYear  1-based
     * @param weekday    the weekday index, 0-based
     */
    public record Date(long dayNumber, long year, int month, int day, int dayOfYear, int weekday, String monthName, String weekdayName, String season) {}

    /** The date of calendar day {@code n}. */
    public static Date dateOf(CalendarDef cal, long n, long epochYear) {
        int len = cal.yearLength();
        long year = epochYear + Math.floorDiv(n, (long) len);
        int doy = (int) Math.floorMod(n, (long) len);
        int month = 0;
        int rest = doy;
        while (rest >= cal.months().get(month).days()) {
            rest -= cal.months().get(month).days();
            month++;
        }
        int wd = (int) Math.floorMod(n, (long) cal.weekdays().size());
        CalendarDef.Month m = cal.months().get(month);
        return new Date(n, year, month, rest + 1, doy + 1, wd, m.name(), cal.weekdays().get(wd), m.season());
    }

    /** The calendar day number of a date. Throws {@link IllegalArgumentException} if the day doesn't exist. */
    public static long dayNumber(CalendarDef cal, long year, int month, int day, long epochYear) {
        if (month < 0 || month >= cal.months().size()) throw new IllegalArgumentException("there is no month " + (month + 1));
        CalendarDef.Month m = cal.months().get(month);
        if (day < 1 || day > m.days()) throw new IllegalArgumentException(m.name() + " has days 1 to " + m.days());
        long before = 0;
        for (int k = 0; k < month; k++) before += cal.months().get(k).days();
        return Math.multiplyExact(year - epochYear, (long) cal.yearLength()) + before + day - 1;
    }

    /**
     * world-days mode: {@code StartDay + floor(worldDays * rate)}. {@code worldDays} counts the world's dawns,
     * so the calendar turns at sunrise.
     */
    public static long worldDays(long startDay, long worldDays, double rate) {
        return startDay + scaled(worldDays, rate);
    }

    /**
     * real-time mode: {@code StartDay + floor((now - epoch + offset) / 1 day * rate)}. The offset is a fixed
     * number of hours: daylight saving time is ignored by design.
     */
    public static long realTime(long startDay, long nowMillis, long epochMillis, double utcOffsetHours, double rate) {
        return startDay + scaled(realDays(nowMillis, epochMillis, utcOffsetHours), rate);
    }

    /** Whole real days since the epoch, at the given UTC offset (day boundaries are local midnight). */
    public static long realDays(long nowMillis, long epochMillis, double utcOffsetHours) {
        long offset = Math.round(utcOffsetHours * MILLIS_PER_HOUR);
        return Math.floorDiv(nowMillis - epochMillis + offset, MILLIS_PER_DAY);
    }

    /** floor(days * rate), exact for rate 1 and stable for whole multiples. */
    static long scaled(long days, double rate) {
        if (rate == 1.0) return days;
        double v = days * rate;
        long r = (long) Math.floor(v);
        // Undo floating-point fuzz just under a whole number (for example 3 * 0.1 * 10).
        if (v - r > 1 - 1e-9) r++;
        return r;
    }

    /** Days from calendar day {@code n} until the season changes, or -1 if the calendar has one season. */
    public static long daysUntilNextSeason(CalendarDef cal, long n, long epochYear) {
        if (cal.seasons().size() < 2) return -1;
        String now = dateOf(cal, n, epochYear).season();
        long k = 1;
        while (k <= cal.yearLength()) {
            if (!dateOf(cal, n + k, epochYear).season().equals(now)) return k;
            k++;
        }
        return -1;
    }
}
