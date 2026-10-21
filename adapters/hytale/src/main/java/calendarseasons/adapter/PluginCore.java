package calendarseasons.adapter;

import calendarseasons.app.CalendarService;
import calendarseasons.app.DateCommands;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

/**
 * Everything the plugin does except the settings hook: wires the store, the clock, chat and {@code /date} to
 * the plain-Java {@link CalendarService}. The main class ({@link CalendarSeasonsPlugin}) adds the hook.
 */
public abstract class PluginCore extends JavaPlugin {
    private static volatile PluginCore instance;

    protected CalendarService service;
    protected HytaleTime time;
    private DateCommands commands;

    protected PluginCore(JavaPluginInit init) {
        super(init);
    }

    /** The running instance (for local tests). */
    public static PluginCore get() {
        return instance;
    }

    public CalendarService service() {
        return service;
    }

    public DateCommands commands() {
        return commands;
    }

    /** The default world's sunrise count. */
    public long dawns() {
        return time.dawns();
    }

    @Override
    protected void setup() {
        instance = this;
        service = new CalendarService(new FlavourStore(getDataDirectory(), getLogger()), new HytaleChat(getLogger()));
        service.start();
        time = new HytaleTime(service);
        commands = new DateCommands(service);
        PermissionsModule.registerPermission(DateCommands.PERMISSION);
        getCommandRegistry().registerCommand(new HytaleCommands(commands, time, getLogger()));
        getEntityStoreRegistry().registerSystem(time.system());
        getLogger().atInfo().log("[CalendarSeasons] calendar %s, clock %s", service.calendar().id(), service.settings().mode());
    }

    @Override
    protected void shutdown() {
        if (service != null) service.shutdown();
    }
}
