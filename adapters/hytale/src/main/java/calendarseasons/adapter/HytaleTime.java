package calendarseasons.adapter;

import calendarseasons.app.CalendarService;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * The default world's clock. The server has no dawn event, so a ticking system reads the world's time each
 * tick, keeps the number of sunrises so far, and lets the service look for a new dawn (or, in real-time
 * mode, local midnight). One calendar per server: other worlds are ignored.
 */
public final class HytaleTime {
    private final CalendarService service;
    /** Sunrises since the world's time began (first sunrise = 1); -1 until the default world has ticked. */
    private volatile long dawns = -1;

    HytaleTime(CalendarService service) {
        this.service = service;
    }

    /** Sunrises since 0001-01-01T00:00 game time; the first sunrise (04:48) is 1. */
    static long dawnIndex(Instant gameTime) {
        long secs = gameTime.getEpochSecond() - WorldTimeResource.ZERO_YEAR.getEpochSecond();
        return Math.floorDiv(secs - WorldTimeResource.SUNRISE_SECONDS, (long) WorldTimeResource.SECONDS_PER_DAY) + 1;
    }

    /** The ticking system to register on the entity store. */
    TickingSystem<EntityStore> system() {
        return new TickingSystem<>() {
            @Override
            public void tick(float dt, int systemIndex, Store<EntityStore> store) {
                World world = store.getExternalData().getWorld();
                Universe u = Universe.get();
                if (u == null || world != u.getDefaultWorld()) return;
                WorldTimeResource time = store.getResource(WorldTimeResource.getResourceType());
                Instant now = time == null ? null : time.getGameTime();
                if (now == null) return;
                long d = dawnIndex(now);
                dawns = d;
                service.tick(d, System.currentTimeMillis());
            }
        };
    }

    /** The default world's sunrise count, read on its thread if the ticker hasn't run yet; 0 without a world. */
    public long dawns() {
        long d = dawns;
        if (d >= 0) return d;
        Universe u = Universe.get();
        World w = u == null ? null : u.getDefaultWorld();
        if (w == null) return 0;
        if (w.isInThread()) return read(w);
        return CompletableFuture.supplyAsync(() -> read(w), w).join();
    }

    private static long read(World w) {
        WorldTimeResource t = w.getEntityStore().getStore().getResource(WorldTimeResource.getResourceType());
        return t == null || t.getGameTime() == null ? 0 : dawnIndex(t.getGameTime());
    }
}
