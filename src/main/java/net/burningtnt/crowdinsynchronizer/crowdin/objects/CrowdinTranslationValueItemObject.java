package net.burningtnt.crowdinsynchronizer.crowdin.objects;

public final class CrowdinTranslationValueItemObject {
    private final int id;

    private final String text;

    private final int rating;

    public CrowdinTranslationValueItemObject(int id, String text, int rating) {
        this.id = id;
        this.text = text;
        this.rating = rating;
    }

    public int getID() {
        return this.id;
    }

    public String getText() {
        return this.text;
    }

    public int getRating() {
        return this.rating;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CrowdinTranslationValueItemObject that = (CrowdinTranslationValueItemObject) o;

        if (id != that.id) return false;
        if (rating != that.rating) return false;
        return text.equals(that.text);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + text.hashCode();
        result = 31 * result + rating;
        return result;
    }
}
