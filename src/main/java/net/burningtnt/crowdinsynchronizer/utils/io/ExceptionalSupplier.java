package net.burningtnt.crowdinsynchronizer.utils.io;

public interface ExceptionalSupplier<T, E extends Throwable> {
    T get() throws E;
}
