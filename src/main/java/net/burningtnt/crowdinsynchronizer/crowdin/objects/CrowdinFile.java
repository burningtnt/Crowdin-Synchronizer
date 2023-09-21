package net.burningtnt.crowdinsynchronizer.crowdin.objects;

public final class CrowdinFile implements Comparable<CrowdinFile> {
    private final int id;

    private final String path;

    private CrowdinFile(int id, String path) {
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

        CrowdinFile that = (CrowdinFile) o;

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
    public int compareTo(CrowdinFile crowdinFile) {
        return this.path.compareTo(crowdinFile == null ? "" : crowdinFile.getPath());
    }
}
