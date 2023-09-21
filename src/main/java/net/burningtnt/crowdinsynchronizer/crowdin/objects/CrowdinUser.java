package net.burningtnt.crowdinsynchronizer.crowdin.objects;

public final class CrowdinUser {
    private final int id;

    private final String username;

    private CrowdinUser(int id, String username) {
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

        CrowdinUser that = (CrowdinUser) o;

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
