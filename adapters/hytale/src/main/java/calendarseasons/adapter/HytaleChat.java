package calendarseasons.adapter;

import calendarseasons.app.CalendarService;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

/** Announcements go to every player's chat and to the server log; warnings go to the log. */
final class HytaleChat implements CalendarService.Out {
    private final HytaleLogger logger;

    HytaleChat(HytaleLogger logger) {
        this.logger = logger;
    }

    @Override
    public void broadcast(String line) {
        logger.atInfo().log("[CalendarSeasons] announce: %s", line);
        Universe u = Universe.get();
        if (u == null) return;
        Message m = Message.raw(line);
        for (PlayerRef p : u.getPlayers()) p.sendMessage(m);
    }

    @Override
    public void info(String line) {
        logger.atInfo().log("%s", line);
    }

    @Override
    public void warn(String line) {
        logger.atWarning().log("%s", line);
    }
}
