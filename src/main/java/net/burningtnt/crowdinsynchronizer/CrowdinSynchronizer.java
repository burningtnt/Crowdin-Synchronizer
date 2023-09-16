package net.burningtnt.crowdinsynchronizer;

import net.burningtnt.crowdinsynchronizer.crowdin.CrowdinAPI;
import net.burningtnt.crowdinsynchronizer.crowdin.CrowdinToken;
import net.burningtnt.crowdinsynchronizer.crowdin.objects.*;
import net.burningtnt.crowdinsynchronizer.datastructure.Column;
import net.burningtnt.crowdinsynchronizer.datastructure.Difference;
import net.burningtnt.crowdinsynchronizer.datastructure.DifferenceType;
import net.burningtnt.crowdinsynchronizer.locali18n.AbstractI18NFile;
import net.burningtnt.crowdinsynchronizer.utils.Lang;
import net.burningtnt.crowdinsynchronizer.utils.io.ExceptionalFunction;
import net.burningtnt.crowdinsynchronizer.utils.io.NetIterator;
import net.burningtnt.crowdinsynchronizer.utils.logger.Logging;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class CrowdinSynchronizer {
    private CrowdinSynchronizer() {
    }

    private static CrowdinUserDataObject getCurrentUser(CrowdinToken token) throws IOException {
        return CrowdinAPI.getCurrentUser(token);
    }

    private static CrowdinProjectObject getSpecificProjectByProjectIdentifier(CrowdinToken token, CrowdinUserDataObject currentUser, String projectIdentifier) throws IOException {
        NetIterator<CrowdinProjectObject> projectIterator = CrowdinAPI.getProjects(token, currentUser);
        while (projectIterator.hasNext()) {
            for (CrowdinProjectObject item : projectIterator.next()) {
                if (item.getIdentifier().equals(projectIdentifier)) {
                    return item;
                }
            }
        }
        throw new IllegalArgumentException(String.format("Cannot find any project with name %s.", projectIdentifier));
    }

    public static void init() {
        Logging.init();
    }

    public static void sync(CrowdinToken token, String projectIdentifier, ExceptionalFunction<String, String, IllegalArgumentException> filePathProvider, AbstractI18NFile sourceLanguage, Collection<AbstractI18NFile> targetLanguages) throws IOException, InterruptedException {
        Logging.getLogger().log(Level.INFO, "Collecting local translations ...");
        sourceLanguage.load();
        for (AbstractI18NFile targetLanguage : targetLanguages) {
            targetLanguage.load();

            for (String targetTranslationKey : targetLanguage.getTranslationKeys()) {
                if (!sourceLanguage.getTranslationKeys().contains(targetTranslationKey)) {
                    Logging.getLogger().log(Level.WARNING, String.format(
                            "Translation for %s contains extra translation key %s, which will be ignored!",
                            targetLanguage.getLanguage(),
                            targetTranslationKey
                    ));
                }
            }
        }

        List<String> targetLanguageIDs = targetLanguages.stream().map(AbstractI18NFile::getLanguage).toList();

        Logging.getLogger().log(Level.INFO, "Logging in ...");
        CrowdinUserDataObject currentUser = getCurrentUser(token);
        Logging.getLogger().log(Level.INFO, String.format("Logged in as %s (%d).", currentUser.getUsername(), currentUser.getID()));

        Logging.getLogger().log(Level.INFO, "Getting specific project ...");
        CrowdinProjectObject project = getSpecificProjectByProjectIdentifier(token, currentUser, projectIdentifier);
        Logging.getLogger().log(Level.INFO, String.format("Current project: %s.", project.getIdentifier()));

        Logging.getLogger().log(Level.INFO, "Collecting crowdin translation files ...");
        Map<String, CrowdinFileObject> crowdinFiles = new ConcurrentSkipListMap<>(); // Crowdin File Path -> Crowdin File Object
        Map<CrowdinFileObject, Column> columns = new TreeMap<>(); // Crowdin File Object -> Column
        CrowdinAPI.getFiles(token, project).forEachRemaining(crowdinFile -> crowdinFiles.put(crowdinFile.getPath(), crowdinFile));
        Logging.getLogger().log(Level.INFO, String.format(
                "Collected crowdin translation files: %s.",
                String.join(", ", crowdinFiles.keySet())
        ));

        Logging.getLogger().log(Level.INFO, "Collecting local translations columns ...");
        for (String sourceTranslationKey : sourceLanguage.getTranslationKeys()) {
            String filePath = filePathProvider.apply(sourceTranslationKey);
            if (crowdinFiles.containsKey("/" + filePath)) {
                CrowdinFileObject crowdinFile = crowdinFiles.get("/" + filePath);
                Column column = columns.get(crowdinFile);
                if (column == null) {
                    column = new Column(crowdinFile);
                    Logging.getLogger().log(Level.INFO, String.format("Collecting crowdin translation keys in file %s ...", filePath));

                    Column columnCopy = column;
                    CrowdinAPI.getTranslationKeys(token, project, crowdinFile).forEachRemaining(
                            crowdinTranslationKey ->
                                    columnCopy.getCrowdinTranslationKeys().put(crowdinTranslationKey.getIdentifier(), crowdinTranslationKey)
                    );
                    columns.put(crowdinFile, column);
                }
                column.getLocalTranslationKeys().add(sourceTranslationKey);
            } else {
                Logging.getLogger().log(Level.INFO, String.format("Missing crowdin translation file %s (for key %s), creating ...", filePath, sourceTranslationKey));
                CrowdinFileObject crowdinFile = CrowdinAPI.addFile(token, project, filePath, targetLanguageIDs);
                crowdinFiles.put(crowdinFile.getPath(), crowdinFile);
                Column column = columns.computeIfAbsent(crowdinFile, Column::new);
                column.getLocalTranslationKeys().add(sourceTranslationKey);
            }
        }

        Logging.getLogger().log(Level.INFO, "Loading differences ...");
        for (Column column : columns.values()) {
            for (String sourceTranslationKey : column.getLocalTranslationKeys()) {
                if (column.getCrowdinTranslationKeys().containsKey(sourceTranslationKey)) {
                    column.pushDifference(DifferenceType.SYNC, sourceTranslationKey);
                } else {
                    column.pushDifference(DifferenceType.ADD, sourceTranslationKey);
                }
            }

            for (String crowdinTranslationKey : column.getCrowdinTranslationKeys().keySet()) {
                if (!column.getLocalTranslationKeys().contains(crowdinTranslationKey)) {
                    column.pushDifference(DifferenceType.REMOVE, crowdinTranslationKey);
                }
            }
        }

        Logging.getLogger().log(Level.INFO, "Delegating differences to threads ...");

        Logging.getLogger().log(Level.INFO, "Processing concurrent actions ...");
        ExecutorService concurrentExecutorService = Executors.newFixedThreadPool(20);
        for (Column column : columns.values()) {
            for (Difference difference : column.getConcurrentDifferences()) {
                concurrentExecutorService.submit(Lang.wrapCheckedException(switch (difference.getType()) {
                    case SYNC -> () -> {
                        for (AbstractI18NFile targetLanguage : targetLanguages) {
                            targetLanguage.setTranslationValue(
                                    difference.getKey(),
                                    CrowdinAPI.getTranslationValue(
                                                    token, project, column.getCrowdinTranslationKeys().get(difference.getKey()), targetLanguage.getLanguage()
                                            ).collectAsList().stream()
                                            .max(Comparator.comparingInt(CrowdinTranslationValueItemObject::getRating))
                                            .map(CrowdinTranslationValueItemObject::getText)
                                            .orElse("")
                            );
                        }

                        Logging.getLogger().log(Level.INFO, String.format("[%s]%s FINISHED", difference.getType().name(), difference.getKey()));

                    };
                    default -> throw new IllegalStateException();
                }));
            }

            column.getConcurrentDifferences().clear();
        }

        for (String path : crowdinFiles.keySet()) {
            CrowdinFileObject file = crowdinFiles.get(path);
            if (!columns.containsKey(file)) {
                concurrentExecutorService.submit(Lang.wrapCheckedException(() -> {
                    CrowdinAPI.deleteFile(token, project, file);
                    crowdinFiles.remove(path);
                    Logging.getLogger().log(Level.INFO, String.format("[REMOVE FILE]%s FINISHED", file.getPath()));
                }));
            }
        }

        concurrentExecutorService.shutdown();
        while (true) {
            boolean finished = concurrentExecutorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if (finished) {
                break;
            }
        }

        ExecutorService blockedExecutorService = Executors.newFixedThreadPool(20);
        Logging.getLogger().log(Level.INFO, "Processing blocked actions ...");
        for (Column column : columns.values()) {
            blockedExecutorService.submit(Lang.wrapCheckedException(() -> {
                for (Difference difference : column.getBlockedDifferences()) {
                    switch (difference.getType()) {
                        case ADD -> {
                            CrowdinTranslationKeyItemObject key = CrowdinAPI.addTranslationKey(token, project, column.getCrowdinFile(), difference.getKey(), sourceLanguage.getTranslationValue(difference.getKey()));
                            column.getCrowdinTranslationKeys().put(key.getIdentifier(), key);
                            for (AbstractI18NFile targetLanguage : targetLanguages) {
                                String value = targetLanguage.getTranslationValue(difference.getKey());
                                CrowdinAPI.addTranslationValue(token, project, key, targetLanguage.getLanguage(), value.length() == 0 ? "/" : value);
                            }

                            Logging.getLogger().log(Level.INFO, String.format("[%s]%s FINISHED", difference.getType().name(), difference.getKey()));
                        }
                        case REMOVE -> {
                            CrowdinAPI.removeTranslationValue(token, project, column.getCrowdinTranslationKeys().get(difference.getKey()));

                            Logging.getLogger().log(Level.INFO, String.format("[%s]%s FINISHED", difference.getType().name(), difference.getKey()));
                        }
                        default -> throw new IllegalStateException();
                    }
                }
            }));
        }

        blockedExecutorService.shutdown();
        while (true) {
            boolean finished = blockedExecutorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if (finished) {
                break;
            }
        }

        Logging.getLogger().log(Level.INFO, "Saving I18N files.");
        sourceLanguage.save();
        for (AbstractI18NFile targetLanguage : targetLanguages) {
            targetLanguage.save();
        }
    }
}
