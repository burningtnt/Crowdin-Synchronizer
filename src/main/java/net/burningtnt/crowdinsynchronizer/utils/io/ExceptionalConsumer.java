package net.burningtnt.crowdinsynchronizer.utils.io;

import java.util.Objects;

public interface ExceptionalConsumer<T, E extends Throwable> {
    void accept(T t) throws E;

    default ExceptionalConsumer<T, E> andThen(ExceptionalConsumer<? super T, E> after) {
        Objects.requireNonNull(after);
        return (T t) -> {
            accept(t);
            after.accept(t);
        };
    }

    static <T> ExceptionalConsumer<T, RuntimeException> identity() {
        return t -> {
        };
    }
}

