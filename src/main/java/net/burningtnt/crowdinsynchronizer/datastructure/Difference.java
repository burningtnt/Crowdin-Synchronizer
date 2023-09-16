package net.burningtnt.crowdinsynchronizer.datastructure;

import java.util.Objects;

public final class Difference {
    private final DifferenceType type;

    private final String key;

    public Difference(DifferenceType type, String keys) {
        this.type = type;
        this.key = keys;
    }

    public DifferenceType getType() {
        return this.type;
    }

    public String getKey() {
        return this.key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Difference that = (Difference) o;

        if (type != that.type) return false;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }
}
