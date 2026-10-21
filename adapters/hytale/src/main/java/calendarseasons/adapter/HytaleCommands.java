package calendarseasons.adapter;

import calendarseasons.app.DateCommands;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /date} and its options. Anyone may read the date; {@code /date set} needs operator status or the
 * {@code calendarseasons.set} permission (the console always may).
 */
final class HytaleCommands extends AbstractAsyncCommand {
    private final DateCommands text;
    private final HytaleTime time;
    private final HytaleLogger logger;

    HytaleCommands(DateCommands text, HytaleTime time, HytaleLogger logger) {
        super(DateCommands.NAME, "Shows the calendar date and season; /date set changes it");
        this.text = text;
        this.time = time;
        this.logger = logger;
        setAllowsExtraArguments(true);
        requireNoPermission();
    }

    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        try {
            boolean maySet = !context.isPlayer() || mayEdit(context);
            List<String> lines = text.run(DateCommands.split(context.getInputString()), maySet, time.dawns(), System.currentTimeMillis());
            for (String l : lines) context.sendMessage(Message.raw(l));
            if (!context.isPlayer()) for (String l : lines) logger.atInfo().log("[CalendarSeasons] /date: %s", l);
        } catch (Throwable t) {
            logger.atSevere().withCause(t).log("[CalendarSeasons] /date failed");
            context.sendMessage(Message.raw("/date failed: " + t.getMessage()));
        }
        return CompletableFuture.completedFuture(null);
    }

    private static boolean mayEdit(CommandContext context) {
        PermissionsModule perms = PermissionsModule.get();
        return perms != null && perms.hasPermission(context.sender().getUuid(), DateCommands.PERMISSION);
    }
}
