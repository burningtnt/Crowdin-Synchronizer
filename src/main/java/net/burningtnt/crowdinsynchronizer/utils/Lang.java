package net.burningtnt.crowdinsynchronizer.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.burningtnt.crowdinsynchronizer.utils.io.ExceptionalConsumer;
import net.burningtnt.crowdinsynchronizer.utils.io.ExceptionalFunction;
import net.burningtnt.crowdinsynchronizer.utils.io.ExceptionalRunnable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Lang {
    private Lang() {
    }

    public static <T, E extends Throwable> T exceptionalTweak(T value, ExceptionalConsumer<T, E> tweaker) throws E {
        tweaker.accept(value);
        return value;
    }

    public static <T> T tweak(T value, Consumer<T> tweaker) {
        tweaker.accept(value);
        return value;
    }

    public static <T, R, E extends Throwable> R mapWithCloseAction(T value, Function<T, R> mapper, ExceptionalConsumer<T, E> closer) throws E {
        R result = mapper.apply(value);
        closer.accept(value);
        return result;
    }

    public static <T, R, E extends Throwable> R exceptionalMapWithCloseAction(T value, ExceptionalFunction<T, R, E> mapper, ExceptionalConsumer<T, E> closer) throws E {
        R result = mapper.apply(value);
        closer.accept(value);
        return result;
    }

    public static JsonObject asJsonObject(Map<String, JsonElement> values) {
        return tweak(new JsonObject(), jsonObject -> values.forEach(jsonObject::add));
    }

    public static <T extends Throwable> Runnable wrapCheckedException(ExceptionalRunnable<T> runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                rethrow(t);
            }
        };
    }

    public static void rethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            if (t.getCause() == null) {
                throw (RuntimeException) t;
            } else {
                rethrow(t.getCause());
            }
        } else if (t instanceof IOException) {
            throw new UncheckedIOException("Rethrow checked exception:" + t.getLocalizedMessage(), (IOException) t);
        } else {
            throw new RuntimeException("Rethrow checked exception:" + t.getLocalizedMessage(), t);
        }
    }
}
