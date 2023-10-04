package net.burningtnt.crowdinsynchronizer.locali18n;

import java.io.IOException;
import java.util.Set;

/**
 * A super class for all kinds of I18N files. It must be thread-safe.
 */
public abstract class AbstractI18NFile {
    /**
     * The language ID of this I18N file. You can look it up on Crowdin.
     * For example: "en" / "zh-CN" / "zh-TW"
     */
    protected final String language;

    protected AbstractI18NFile(String language) {
        this.language = language;
    }

    public final String getLanguage() {
        return this.language;
    }

    /**
     * CrowdinSyncronizer would invoke this method when it's time to load all the translation key-value pairs from this file.
     * You need to read the key-value pairs from a file, and stored them in a thread-safe map as
     * {@link AbstractI18NFile#getTranslationKeys()}, {@link AbstractI18NFile#getTranslationValue(String)} and
     * {@link AbstractI18NFile#setTranslationValue(String, String)} would be invoked from multi-thread.
     *
     * @throws IOException Throw an IOException if an exception is encountered while loading the file.
     */
    public abstract void load() throws IOException;

    /**
     * CrowdinSyncronizer would invoke this method when it's time to save all the translation key-value pairs from this file.
     * After this method is invoked, you should clear the storage to reduce memory usage.
     *
     * @throws IOException Throw an IOException if an exception is encountered while saving the file.
     */
    public abstract void save() throws IOException;

    /**
     * This method is used to get all the translation keys which exist in this file.
     * If this method is invoked before {@link AbstractI18NFile#load()} or after {@link AbstractI18NFile#save()},
     * You should throw an {@link IllegalStateException}.
     *
     * @return A unmodifiable set contains all the existed translation keys in this file.
     */
    public abstract Set<String> getTranslationKeys();

    /**
     * This method is used to get a translation value from the key.
     * @param key The translation key.
     * @return The matching translation value. If the key doesn't exist, return "". DO NOT RETURN NULL.
     */
    public abstract String getTranslationValue(String key);

    /**
     * This method is used to set a translation key-value pair.
     * If the key doesn't exist before, please allocate its space.
     *
     * @param key The translation key.
     * @param value The translaiotn value.
     */
    public abstract void setTranslationValue(String key, String value);
}
