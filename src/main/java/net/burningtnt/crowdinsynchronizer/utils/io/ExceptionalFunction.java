package net.burningtnt.crowdinsynchronizer.utils.io;

import java.util.Objects;

public interface ExceptionalFunction<T, R, E extends Throwable> {
    R apply(T t) throws E;

    default <V> ExceptionalFunction<V, R, E> compose(ExceptionalFunction<? super V, ? extends T, E> before) {
        Objects.requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }

    default <V> ExceptionalFunction<T, V, E> andThen(ExceptionalFunction<? super R, ? extends V, E> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }

    static <T, E extends Throwable> ExceptionalFunction<T, T, E> identity() {
        return t -> t;
    }
}
