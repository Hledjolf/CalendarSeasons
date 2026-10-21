package calendarseasons.adapter;

import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.Map;

/**
 * Calendar &amp; Seasons, the main class named in manifest.json. It lives in the adapter package, the only
 * package that names the Hytale API; the work is in {@link PluginCore} and the plain-Java service.
 */
public final class CalendarSeasonsPlugin extends PluginCore {
    public CalendarSeasonsPlugin(JavaPluginInit init) {
        super(init);
    }

    /**
     * The settings hook, called by a settings tool such as Mod Settings Manager after it rewrote config.json
     * (found by reflection; no dependency). Re-reads the file and rebuilds the calendar, and returns true:
     * every setting applies live. If the file can't be read it throws, with the reason, because a restart
     * would not help; the server log has the same line.
     */
    public boolean onSettingsChanged(Map<String, Object> changed) {
        return service.settingsChanged(time.dawns(), System.currentTimeMillis());
    }
}
