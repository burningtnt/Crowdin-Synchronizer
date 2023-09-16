package net.burningtnt.crowdinsynchronizer.crowdin.objects;

public class CrowdinStorageObject {
    private final int id;

    private final String fileName;

    public CrowdinStorageObject(int id, String fileName) {
        this.id = id;
        this.fileName = fileName;
    }

    public int getID() {
        return this.id;
    }

    public String getFileName() {
        return this.fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CrowdinStorageObject that = (CrowdinStorageObject) o;

        if (id != that.id) return false;
        return fileName.equals(that.fileName);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + fileName.hashCode();
        return result;
    }
}
