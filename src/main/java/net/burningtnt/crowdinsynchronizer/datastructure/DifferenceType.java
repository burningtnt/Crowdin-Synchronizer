package net.burningtnt.crowdinsynchronizer.datastructure;

public enum DifferenceType {
    ADD(false),
    SYNC(true),
    REMOVE(false);

    private final boolean allowConcurrency;

    DifferenceType(boolean allowConcurrency) {
        this.allowConcurrency = allowConcurrency;
    }

    public boolean isAllowConcurrency() {
        return this.allowConcurrency;
    }
}
