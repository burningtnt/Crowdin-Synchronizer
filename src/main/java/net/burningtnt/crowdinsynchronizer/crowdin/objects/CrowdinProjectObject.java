package net.burningtnt.crowdinsynchronizer.crowdin.objects;

public final class CrowdinProjectObject {
    private final int id;

    private final String name;

    private final String identifier;

    public CrowdinProjectObject(int id, String name, String identifier) {
        this.id = id;
        this.name = name;
        this.identifier = identifier;
    }

    public int getID() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CrowdinProjectObject that = (CrowdinProjectObject) o;

        if (id != that.id) return false;
        if (!name.equals(that.name)) return false;
        return identifier.equals(that.identifier);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + name.hashCode();
        result = 31 * result + identifier.hashCode();
        return result;
    }
}
