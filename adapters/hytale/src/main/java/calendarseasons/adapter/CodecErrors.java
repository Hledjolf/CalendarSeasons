package calendarseasons.adapter;

import com.hypixel.hytale.codec.exception.CodecException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/** Turns the codec's nested load failures into one readable message. */
final class CodecErrors {
    private CodecErrors() {}

    /** Waits for a config load; on failure throws an exception whose message names the bad key and why. */
    static <T> T join(CompletableFuture<T> f) throws Exception {
        try {
            return f.get();
        } catch (ExecutionException | CompletionException e) {
            throw new Exception(describe(e.getCause() == null ? e : e.getCause()), e);
        }
    }

    static String describe(Throwable t) {
        String key = null;
        Throwable root = t;
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof CodecException && c.getMessage() != null) {
                String m = c.getMessage();
                int a = m.indexOf('\''), b = a < 0 ? -1 : m.indexOf('\'', a + 1);
                if (a >= 0 && b > a) key = m.substring(a + 1, b); // the innermost key wins
            }
            root = c;
        }
        String why = root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage().strip();
        if (why.contains("\n")) why = why.substring(0, why.indexOf('\n'));
        return key == null ? why : "the value of " + key + " is not valid (" + why + ")";
    }
}
