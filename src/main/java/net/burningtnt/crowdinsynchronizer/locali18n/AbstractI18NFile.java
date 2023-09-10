package net.burningtnt.crowdinsynchronizer.locali18n;

import java.io.IOException;
import java.util.Set;

public abstract class AbstractI18NFile {
    protected final String language;

    protected AbstractI18NFile(String language) {
        this.language = language;
    }

    public abstract void load() throws IOException;

    public abstract void save() throws IOException;

    public abstract Set<String> getTranslationKeys();

    public final String getLanguage() {
        return this.language;
    }

    public abstract String getTranslationValue(String key);

    public abstract void setTranslationValue(String key, String value);
}
