package net.burningtnt.crowdinsynchronizer.crowdin.objects;

public final class CrowdinFileObject implements Comparable<CrowdinFileObject> {
    private final int id;

    private final String path;

    public CrowdinFileObject(int id, String path) {
        this.id = id;
        this.path = path;
    }

    public int getID() {
        return this.id;
    }

    public String getPath() {
        return this.path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CrowdinFileObject that = (CrowdinFileObject) o;

        if (id != that.id) return false;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + path.hashCode();
        return result;
    }

    @Override
    public int compareTo(CrowdinFileObject crowdinFileObject) {
        return this.path.compareTo(crowdinFileObject == null ? "" : crowdinFileObject.getPath());
    }
}
