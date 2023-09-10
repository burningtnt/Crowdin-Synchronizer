package net.burningtnt.crowdinsynchronizer.crowdin;

public final class CrowdinToken {
    private final String token;

    private CrowdinToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return this.token;
    }

    public static CrowdinToken of(String token) {
        return new CrowdinToken(token);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CrowdinToken that = (CrowdinToken) o;

        return token.equals(that.token);
    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }
}
