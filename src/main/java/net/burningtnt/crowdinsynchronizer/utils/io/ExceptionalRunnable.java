package net.burningtnt.crowdinsynchronizer.utils.io;

public interface ExceptionalRunnable<E extends Throwable> {
    void run() throws E;
}
