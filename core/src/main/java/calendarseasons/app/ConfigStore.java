package calendarseasons.app;

import calendarseasons.core.Settings;
import calendarseasons.core.State;

/** Where the settings and the remembered state live. The adapter supplies the implementation. */
public interface ConfigStore {
    /** Reads config.json (writing the defaults first if there is none). Throws with a plain message if it can't. */
    Settings loadSettings() throws Exception;

    /** The remembered state, or a fresh one. */
    State loadState() throws Exception;

    /** Keeps the state (where and when depends on the store). */
    void saveState(State state) throws Exception;

    /** Called once when the server stops. */
    void shutdown(State state);

    /** The config file, for log lines. */
    String configPath();
}
