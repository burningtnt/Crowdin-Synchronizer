package net.burningtnt.crowdinsynchronizer.locali18n;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

public final class PropertiesI18NFile extends AbstractI18NFile {
    private final Path path;

    private final Map<String, String> translations = new ConcurrentSkipListMap<>();

    public PropertiesI18NFile(String language, Path path) {
        super(language);
        this.path = path;
    }

    public void load() throws IOException {
        Properties properties = new Properties();
        properties.load(Files.newBufferedReader(path, StandardCharsets.UTF_8));
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (entry.getKey() instanceof String key) {
                if (entry.getValue() instanceof String value) {
                    translations.put(key, value);
                }
            }
        }
    }

    public void save() throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                saveConvert(writer, entry.getKey());
                writer.write('=');
                saveConvert(writer, entry.getValue());
                writer.write('\r');
                writer.write('\n');
            }
        }
    }

    private void saveConvert(Writer writer, String raw) throws IOException {
        int len = raw.length();
        for (int x = 0; x < len; x++) {
            char aChar = raw.charAt(x);
            if ((aChar > 61) && (aChar < 127)) {
                if (aChar == '\\') {
                    writer.append('\\');
                    writer.append('\\');
                    continue;
                }
                writer.append(aChar);
                continue;
            }
            switch (aChar) {
                case '\t' -> {
                    writer.append('\\');
                    writer.append('t');
                }
                case '\n' -> {
                    writer.append('\\');
                    writer.append('n');
                }
                case '\r' -> {
                    writer.append('\\');
                    writer.append('r');
                }
                case '\f' -> {
                    writer.append('\\');
                    writer.append('f');
                }
                case '=', ':', '#', '!' -> {
                    writer.append('\\');
                    writer.append(aChar);
                }
                default -> writer.append(aChar);
            }
        }
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
