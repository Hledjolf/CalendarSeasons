package calendarseasons.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The bundled calendars, read from {@code calendars/<id>.json} on the class path. Calendars are data: a
 * preset is the same shape as a custom calendar.
 */
public final class Presets {
    public static final String HARVEST = "harvest";
    /** The preset used when nothing valid is configured. */
    public static final String DEFAULT = "gregorian";
    public static final String CUSTOM = "custom";
    /** The bundled preset ids, in menu order (custom is not a file). */
    public static final List<String> IDS = List.of("gregorian", HARVEST, "norse");

    private Presets() {}

    /** A bundled preset. Throws {@link IllegalArgumentException} for an unknown id or a broken file. */
    public static CalendarDef load(String id) {
        if (!IDS.contains(id)) throw new IllegalArgumentException("no preset " + id);
        String path = "calendars/" + id + ".json";
        try (InputStream in = Presets.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalArgumentException("missing resource " + path);
            CalendarDef c = CalendarDef.fromJson(Json.parseObject(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            if (!id.equals(c.id())) throw new IllegalArgumentException(path + " has Id " + c.id());
            return c;
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot read " + path + ": " + e.getMessage(), e);
        }
    }
}
