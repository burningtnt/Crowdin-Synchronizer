package net.burningtnt.crowdinsynchronizer.utils;

import com.google.gson.*;
import net.burningtnt.crowdinsynchronizer.utils.io.ExceptionalConsumer;
import net.burningtnt.crowdinsynchronizer.utils.io.ExceptionalRunnable;
import net.burningtnt.crowdinsynchronizer.utils.io.ExceptionalSupplier;
import net.burningtnt.crowdinsynchronizer.utils.logger.Logging;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public final class Lang {
    private Lang() {
    }

    private static final Gson GSON = new GsonBuilder().create();

    public static <T, E extends Throwable> T exceptionalTweak(T value, ExceptionalConsumer<T, E> tweaker) throws E {
        tweaker.accept(value);
        return value;
    }

    public static <T> T tweak(T value, Consumer<T> tweaker) {
        tweaker.accept(value);
        return value;
    }

    public static Gson getGson() {
        return GSON;
    }

    public static JsonObject asJsonObject(Map<String, JsonElement> values) {
        return tweak(new JsonObject(), jsonObject -> values.forEach(jsonObject::add));
    }

    public static <T, E extends Throwable> T tryInvoke(ExceptionalSupplier<T, E> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    public static <T, E extends Throwable> T tryInvokeWithHandle(ExceptionalSupplier<T, E> supplier, Function<Throwable, T> defaultValue) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            return defaultValue.apply(t);
        }
    }

    public static <T extends Throwable> Runnable wrapCheckedException(ExceptionalRunnable<T> runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                Logging.getLogger().log(Level.WARNING, "An Error encountered.", t);
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
            throw new UncheckedIOException("Rethrowed checked exception:" + t.getLocalizedMessage(), (IOException) t);
        } else {
            throw new RuntimeException("Rethrowed checked exception:" + t.getLocalizedMessage(), t);
        }
    }

    public static String readAllBytesAsString(InputStreamReader reader) throws IOException {
        StringWriter writer = new StringWriter();
        reader.transferTo(writer);
        return writer.getBuffer().toString();
    }

    public static String readAllBytesAsString(ExceptionalSupplier<InputStreamReader, IOException> supplier) throws IOException {
        try (InputStreamReader reader = supplier.get()) {
            return readAllBytesAsString(reader);
        }
    }

    public static String getExceptionMessage(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        t.printStackTrace(printWriter);
        printWriter.close();
        return stringWriter.getBuffer().toString();
    }

    public static void concurrentProcess(int threadN, Consumer<ExecutorService> processor) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadN);

        processor.accept(executorService);

        executorService.shutdown();
        while (true) {
            boolean finished = executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if (finished) {
                break;
            }
        }
    }

    public static Process joinProcess(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        Process process = processBuilder.inheritIO().start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException(String.format("Process %s returns %d.", process, exitCode));
        }
        return process;
    }
}
