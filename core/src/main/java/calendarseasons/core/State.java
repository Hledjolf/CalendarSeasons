package calendarseasons.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the mod remembers between restarts, kept apart from the settings in {@code state.json}:
 * {@code { "Version": 1, "StartDay": 0, "LastSeason": "Spring" }}. {@code StartDay} is what
 * {@code /date set} moves; {@code LastSeason} lets a season change be announced once.
 */
public record State(int version, long startDay, String lastSeason) {
    public static final int VERSION = 1;

    public static State fresh() {
        return new State(VERSION, 0, null);
    }

    public State withStartDay(long d) {
        return new State(version, d, lastSeason);
    }

    public State withLastSeason(String s) {
        return new State(version, startDay, s);
    }

    /** Reads state.json text. Unknown keys are ignored; a newer Version is refused. */
    public static State fromJson(String text) {
        Map<String, Object> o = Json.parseObject(text);
        int v = o.get("Version") instanceof Number n ? n.intValue() : VERSION;
        if (v > VERSION) throw new IllegalArgumentException("state.json Version " + v + " is newer than this mod understands (" + VERSION + ")");
        long start = o.get("StartDay") instanceof Number n ? n.longValue() : 0;
        String last = o.get("LastSeason") instanceof String s ? s : null;
        return new State(VERSION, start, last);
    }

    public String toJson() {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("Version", VERSION);
        o.put("StartDay", startDay);
        o.put("LastSeason", lastSeason);
        return Json.write(o);
    }
}
