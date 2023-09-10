package net.burningtnt.crowdinsynchronizer.crowdin.objects;

public final class CrowdinUserDataObject {
    private final int id;

    private final String username;

    public CrowdinUserDataObject(int id, String username) {
        this.id = id;
        this.username = username;
    }

    public int getID() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CrowdinUserDataObject that = (CrowdinUserDataObject) o;

        if (id != that.id) return false;
        return username.equals(that.username);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + username.hashCode();
        return result;
    }
}
