package net.burningtnt.crowdinsynchronizer.locali18n;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

public final class EmptyI18NFile extends AbstractI18NFile {
    public EmptyI18NFile(String language) {
        super(language);
    }

    private final Map<String, String> translations = new ConcurrentSkipListMap<>();

    @Override
    public void load() {
    }

    @Override
    public void save() {
    }

    @Override
    public Set<String> getTranslationKeys() {
        return this.translations.keySet();
    }

    @Override
    public String getTranslationValue(String key) {
        return this.translations.getOrDefault(key, "");
    }

    @Override
    public void setTranslationValue(String key, String value) {
        this.translations.put(key, value);
    }
}
