package calendarseasons.adapter;

import calendarseasons.app.ConfigStore;
import calendarseasons.core.Settings;
import calendarseasons.core.State;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.util.Config;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * The published store. {@code config.json} is read through the server's own config codec; it is written
 * only once, with the defaults, when there is no file yet (the codec's save drops unknown keys and reorders
 * the file, so the mod never saves it again). The calendar day lives in {@code state.json}, so a settings
 * tool's edits are never put back.
 *
 * <p>The config is loaded inside a try instead of with {@code withConfig}: a decode error there stops the
 * whole server from booting, while here it costs one log line and the defaults.
 */
final class FlavourStore implements ConfigStore {
    static final BuilderCodec<ConfigCodec.Root> CODEC = ConfigCodec.codec(false);

    private final Path dir;
    private final HytaleLogger logger;

    FlavourStore(Path dataDirectory, HytaleLogger logger) {
        this.dir = dataDirectory;
        this.logger = logger;
    }

    private Path configFile() {
        return dir.resolve("config.json");
    }

    private Path stateFile() {
        return dir.resolve("state.json");
    }

    @Override
    public String configPath() {
        return configFile().toString();
    }

    @Override
    public Settings loadSettings() throws Exception {
        Files.createDirectories(dir);
        boolean fresh = !Files.exists(configFile());
        // A new Config each time: after a failed load, Config.load() keeps returning the same failed future.
        Config<ConfigCodec.Root> config = new Config<>(dir, "config", CODEC);
        ConfigCodec.Root root = CodecErrors.join(config.load());
        if (fresh) {
            config.save().join();
            logger.atInfo().log("[CalendarSeasons] wrote the default settings to %s", configFile());
        }
        return ConfigCodec.toSettings(root);
    }

    @Override
    public State loadState() throws Exception {
        if (!Files.exists(stateFile())) return State.fresh();
        return State.fromJson(Files.readString(stateFile(), StandardCharsets.UTF_8));
    }

    @Override
    public void saveState(State state) throws Exception {
        Files.createDirectories(dir);
        Path tmp = dir.resolve("state.json.tmp");
        Files.writeString(tmp, state.toJson(), StandardCharsets.UTF_8);
        try {
            Files.move(tmp, stateFile(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tmp, stateFile(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void shutdown(State state) {
        // Nothing to do: the state is saved when it changes, and config.json is never written back.
    }
}
