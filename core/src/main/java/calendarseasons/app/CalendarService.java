package calendarseasons.app;

import calendarseasons.core.CalendarDef;
import calendarseasons.core.DateMath;
import calendarseasons.core.Formatter;
import calendarseasons.core.Presets;
import calendarseasons.core.Settings;
import calendarseasons.core.State;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The running calendar: the current settings, the calendar built from them, the remembered state, and dawn
 * and season detection. Plain Java; the adapter feeds it the world's dawn count and the real time.
 * Thread-safe (every public method is synchronized).
 */
public final class CalendarService {
    /** What the service tells the outside world. */
    public interface Out {
        void broadcast(String line);

        void info(String line);

        void warn(String line);
    }

    private final ConfigStore store;
    private final Out out;
    private final List<Consumer<String>> broadcastListeners = new CopyOnWriteArrayList<>();

    private Settings settings = Settings.defaults();
    private CalendarDef calendar = Presets.load(Presets.HARVEST);
    private State state = State.fresh();
    /** The last dawn key seen (world dawns, or real local days); null until the first tick. */
    private Long lastDawnKey;

    public CalendarService(ConfigStore store, Out out) {
        this.store = store;
        this.out = out;
    }

    /** Loads settings and state at startup. Problems are logged; the mod always ends up running. */
    public synchronized void start() {
        try {
            apply(store.loadSettings());
        } catch (Exception e) {
            out.warn("[CalendarSeasons] cannot read " + store.configPath() + ": " + e.getMessage() + ". Using the default settings until it is fixed.");
        }
        try {
            state = store.loadState();
        } catch (Exception e) {
            out.warn("[CalendarSeasons] cannot read the saved calendar day (" + e.getMessage() + "); starting from day 0");
            state = State.fresh();
        }
    }

    /**
     * Re-reads config.json and rebuilds the calendar. Returns null when the new settings are in force, or the
     * reason the file couldn't be read (the previous settings then stay in force).
     */
    public synchronized String reloadSettings() {
        Settings loaded;
        try {
            loaded = store.loadSettings();
        } catch (Exception e) {
            String why = "cannot read " + store.configPath() + ": " + e.getMessage() + ". Keeping the previous settings.";
            out.warn("[CalendarSeasons] " + why);
            return why;
        }
        apply(loaded);
        return null;
    }

    /** Uses these settings now (checked, with one log line per fixed value). */
    public synchronized void apply(Settings raw) {
        Settings.Checked c = raw.checked();
        for (String f : c.fixes()) out.warn("[CalendarSeasons] " + f);
        String oldMode = settings.mode();
        settings = c.settings();
        calendar = buildCalendar(settings, out);
        if (!Objects.equals(oldMode, settings.mode())) lastDawnKey = null; // a mode switch is not a dawn
    }

    /** The calendar for these settings; an invalid custom calendar falls back to harvest with one log line. */
    static CalendarDef buildCalendar(Settings s, Out out) {
        if (Presets.CUSTOM.equals(s.preset())) {
            List<String> problems = CalendarDef.problems(s.customMonths(), s.customWeekdays());
            if (problems.isEmpty()) return CalendarDef.of(Presets.CUSTOM, s.customMonths(), s.customWeekdays(), true, null);
            out.warn("[CalendarSeasons] the custom calendar can't be used (" + String.join("; ", problems) + "); using the harvest calendar instead");
            return Presets.load(Presets.HARVEST);
        }
        try {
            return Presets.load(s.preset());
        } catch (IllegalArgumentException e) {
            out.warn("[CalendarSeasons] preset " + s.preset() + " is broken (" + e.getMessage() + "); using harvest");
            return Presets.load(Presets.HARVEST);
        }
    }

    /**
     * The settings hook's work, after a settings tool rewrote config.json: reload, and adopt today's season
     * silently (a season that changes because of an edit is not announced; only dawn announces). Throws
     * {@link IllegalStateException} with the reason if the file can't be read: a restart wouldn't help, so
     * the settings tool should report the change as not applied.
     */
    public synchronized boolean settingsChanged(long worldDawns, long nowMillis) {
        String problem = reloadSettings();
        if (problem != null) throw new IllegalStateException(problem);
        String season = today(worldDawns, nowMillis).season();
        if (!season.equals(state.lastSeason())) saveState(state.withLastSeason(season));
        return true;
    }

    public synchronized Settings settings() {
        return settings;
    }

    public synchronized CalendarDef calendar() {
        return calendar;
    }

    public synchronized State state() {
        return state;
    }

    public void addBroadcastListener(Consumer<String> l) {
        broadcastListeners.add(l);
    }

    public void removeBroadcastListener(Consumer<String> l) {
        broadcastListeners.remove(l);
    }

    /** The calendar day number now. */
    public synchronized long dayNumber(long worldDawns, long nowMillis) {
        Settings s = settings;
        return s.realTime()
            ? DateMath.realTime(state.startDay(), nowMillis, s.realTimeEpochMillis(), s.utcOffsetHours(), s.calendarDaysPerWorldDay())
            : DateMath.worldDays(state.startDay(), worldDawns, s.calendarDaysPerWorldDay());
    }

    public synchronized DateMath.Date today(long worldDawns, long nowMillis) {
        return DateMath.dateOf(calendar, dayNumber(worldDawns, nowMillis), settings.epochYear());
    }

    /** A date written with the configured format and switches. */
    public synchronized String format(DateMath.Date d) {
        Settings s = settings;
        return Formatter.format(d, calendar, s.dateFormat(), s.yearLabel(), s.showWeekday(), s.showSeason());
    }

    /**
     * Called often (each world tick). Detects dawn: in world-days mode a new world sunrise, in real-time mode
     * local midnight. The first call only records where the clock is; a clock moved backwards is not a dawn.
     */
    public synchronized void tick(long worldDawns, long nowMillis) {
        long key = settings.realTime() ? DateMath.realDays(nowMillis, settings.realTimeEpochMillis(), settings.utcOffsetHours()) : worldDawns;
        Long prev = lastDawnKey;
        lastDawnKey = key;
        if (prev == null) {
            if (state.lastSeason() == null) saveState(state.withLastSeason(today(worldDawns, nowMillis).season()));
            return;
        }
        if (key > prev) dawn(worldDawns, nowMillis);
    }

    private void dawn(long worldDawns, long nowMillis) {
        DateMath.Date d = today(worldDawns, nowMillis);
        if (settings.announceAtDawn()) say(settings.announceTemplate().replace("{date}", format(d)));
        if (!d.season().equals(state.lastSeason())) {
            if (settings.announceSeasonChange() && state.lastSeason() != null) say(calendar.seasonTemplate().replace("{season}", d.season()));
            saveState(state.withLastSeason(d.season()));
        }
    }

    private void say(String line) {
        out.broadcast(line);
        for (Consumer<String> l : broadcastListeners) l.accept(line);
    }

    /** Moves StartDay so that today becomes the given date. */
    public synchronized DateMath.Date setDate(long year, int month, int day, long worldDawns, long nowMillis) {
        long target = DateMath.dayNumber(calendar, year, month, day, settings.epochYear());
        long now = dayNumber(worldDawns, nowMillis);
        State next = state.withStartDay(Math.addExact(state.startDay(), target - now));
        DateMath.Date d = DateMath.dateOf(calendar, target, settings.epochYear());
        saveState(next.withLastSeason(d.season()));
        return d;
    }

    private void saveState(State s) {
        state = s;
        try {
            store.saveState(s);
        } catch (Exception e) {
            out.warn("[CalendarSeasons] cannot save the calendar state: " + e.getMessage());
        }
    }

    /** Called once when the server stops. */
    public synchronized void shutdown() {
        store.shutdown(state);
    }
}
