package net.burningtnt.crowdinsynchronizer;

import net.burningtnt.crowdinsynchronizer.crowdin.CrowdinAPI;
import net.burningtnt.crowdinsynchronizer.crowdin.CrowdinToken;
import net.burningtnt.crowdinsynchronizer.crowdin.objects.*;
import net.burningtnt.crowdinsynchronizer.locali18n.AbstractI18NFile;
import net.burningtnt.crowdinsynchronizer.utils.Lang;
import net.burningtnt.crowdinsynchronizer.utils.io.ExceptionalFunction;
import net.burningtnt.crowdinsynchronizer.utils.io.NetIterator;
import net.burningtnt.crowdinsynchronizer.utils.logger.Logging;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

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

    private static CrowdinFileObject getSpecificFileByFilePath(CrowdinToken token, CrowdinProjectObject project, String filePath) throws IOException {
        NetIterator<CrowdinFileObject> fileIterator = CrowdinAPI.getFiles(token, project);
        while (fileIterator.hasNext()) {
            for (CrowdinFileObject item : fileIterator.next()) {
                if (item.getPath().equals(filePath)) {
                    return item;
                }
            }
        }
        throw new IllegalArgumentException(String.format("Cannot find any file with path %s.", filePath));
    }

    public static void init() {
        Logging.init();
    }

    public static void sync(CrowdinToken token, String projectIdentifier, ExceptionalFunction<Character, String, IllegalArgumentException> filePathProvider, AbstractI18NFile sourceLanguage, Collection<AbstractI18NFile> targetLanguages) throws IOException, InterruptedException {
        sourceLanguage.load();
        for (String sourceTranslationKey : sourceLanguage.getTranslationKeys()) {
            char column = sourceTranslationKey.charAt(0);
            if (column > 'z' || column < 'a') {
                throw new IllegalArgumentException(String.format("Illegal translation key %s in translation for %s.", sourceTranslationKey, sourceLanguage.getLanguage()));
            }
        }
        for (AbstractI18NFile targetLanguage : targetLanguages) {
            targetLanguage.load();

            for (String targetTranslationKey : targetLanguage.getTranslationKeys()) {
                char column = targetTranslationKey.charAt(0);
                if (column > 'z' || column < 'a') {
                    throw new IllegalArgumentException(String.format("Illegal translation key %s in translation for %s.", targetTranslationKey, targetLanguage.getLanguage()));
                }

                if (!sourceLanguage.getTranslationKeys().contains(targetTranslationKey)) {
                    Logging.getLogger().log(Level.WARNING, String.format(
                            "Translation for %s contains extra translation key %s, which will be ignored!",
                            targetLanguage.getLanguage(),
                            targetTranslationKey
                    ));
                }
            }
        }

        Logging.getLogger().log(Level.INFO, "Logging in ...");
        CrowdinUserDataObject currentUser = getCurrentUser(token);
        Logging.getLogger().log(Level.INFO, String.format("Logged in as %s (%d).", currentUser.getUsername(), currentUser.getID()));

        Logging.getLogger().log(Level.INFO, "Getting specific project ...");
        CrowdinProjectObject project = getSpecificProjectByProjectIdentifier(token, currentUser, projectIdentifier);
        Logging.getLogger().log(Level.INFO, String.format("Current project: %s.", project.getIdentifier()));

        Logging.getLogger().log(Level.INFO, "Getting specific file ...");
        Map<Character, CrowdinFileObject> files = new TreeMap<>();
        for (Character c : "abcdefghijklmnopqrstuvwxyz".toCharArray()) {
            files.put(c, getSpecificFileByFilePath(token, project, filePathProvider.apply(c)));
        }

        Logging.getLogger().log(Level.INFO, String.format("Current files: {%s}.", files.values().stream().map(CrowdinFileObject::getPath).collect(Collectors.joining(", "))));

        Logging.getLogger().log(Level.INFO, "Collecting crowdin translation keys ...");
        Map<Character, Map<String, CrowdinTranslationKeyItemObject>> crowdinTranslationKeys = new TreeMap<>();
        for (Character c : "abcdefghijklmnopqrstuvwxyz".toCharArray()) {
            crowdinTranslationKeys.put(c, new TreeMap<>());
        }
        for (Map.Entry<Character, CrowdinFileObject> entry : files.entrySet()) {
            CrowdinAPI.getTranslationKeys(token, project, entry.getValue()).forEachRemaining(key -> crowdinTranslationKeys.get(entry.getKey()).put(key.getIdentifier(), key));
        }
        Logging.getLogger().log(Level.INFO, String.format(
                "Crowdin translation keys: \n%s\n.",
                crowdinTranslationKeys.entrySet().stream()
                        .map(entry -> String.format("    %s: {%s}", entry.getKey(), String.join(", ", entry.getValue().keySet())))
                        .collect(Collectors.joining("\n"))
        ));

        Logging.getLogger().log(Level.INFO, "Loading differences ...");
        Map<Character, Map<String, DifferenceType>> differences = new ConcurrentSkipListMap<>();
        for (Character c : "abcdefghijklmnopqrstuvwxyz".toCharArray()) {
            differences.put(c, new TreeMap<>());
        }

        for (String sourceTranslationKey : sourceLanguage.getTranslationKeys()) {
            Character column = sourceTranslationKey.charAt(0);
            if (!crowdinTranslationKeys.get(column).containsKey(sourceTranslationKey)) {
                differences.get(column).put(sourceTranslationKey, DifferenceType.ADD);
            } else {
                differences.get(column).put(sourceTranslationKey, DifferenceType.SYNC);
            }
        }

        for (Character column : crowdinTranslationKeys.keySet()) {
            for (String crowdinTranslationKey : crowdinTranslationKeys.get(column).keySet()) {
                if (!sourceLanguage.getTranslationKeys().contains(crowdinTranslationKey)) {
                    differences.get(column).put(crowdinTranslationKey, DifferenceType.REMOVE);
                }
            }
        }

        Logging.getLogger().log(Level.INFO, String.format(
                "Differences: \n%s\n",
                differences.entrySet().stream()
                        .map(entry -> String.format("    %s: {%s}", entry.getKey(), entry.getValue().entrySet().stream().map(differenceItem -> String.format("[%s]%s", differenceItem.getValue().name(), differenceItem.getKey())).collect(Collectors.joining(", "))))
                        .collect(Collectors.joining("\n"))
        ));

        Logging.getLogger().log(Level.INFO, "Delegating differences to threads ...");

        Logging.getLogger().log(Level.INFO, "Processing concurrent actions ...");
        ExecutorService concurrentExecutorService = Executors.newFixedThreadPool(20);
        for (Map.Entry<Character, Map<String, DifferenceType>> entry : differences.entrySet()) {
            if (entry.getValue().size() == 0) {
                continue;
            }

            for (Map.Entry<String, DifferenceType> diffItem : entry.getValue().entrySet()) {
                if (!diffItem.getValue().isAllowConcurrency()) {
                    continue;
                }
                Map<String, CrowdinTranslationKeyItemObject> currentCrowdinTranslationKeys = crowdinTranslationKeys.get(entry.getKey());

                switch (diffItem.getValue()) {
                    case SYNC -> {
                        concurrentExecutorService.submit(Lang.wrapCheckedException(() -> {
                            for (AbstractI18NFile targetLanguage : targetLanguages) {
                                targetLanguage.setTranslationValue(
                                        diffItem.getKey(),
                                        CrowdinAPI.getTranslationValue(
                                                        token, project, currentCrowdinTranslationKeys.get(diffItem.getKey()), targetLanguage.getLanguage()
                                                ).collectAsList().stream()
                                                .max(Comparator.comparingInt(CrowdinTranslationValueItemObject::getRating))
                                                .map(CrowdinTranslationValueItemObject::getText)
                                                .orElse("")
                                );
                            }

                            Logging.getLogger().log(Level.INFO, String.format("~ %s - FINISHED", diffItem.getKey()));
                        }));
                    }
                }
            }
        }

        concurrentExecutorService.shutdown();
        while (true) {
            boolean finished = concurrentExecutorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if (finished) {
                break;
            }
        }

        ExecutorService unConcurrentExecutorService = Executors.newFixedThreadPool(20);

        for (Map.Entry<Character, Map<String, DifferenceType>> entry : differences.entrySet()) {
            if (entry.getValue().size() == 0) {
                continue;
            }

            CrowdinFileObject file = files.get(entry.getKey());
            Map<String, DifferenceType> currentDifferences = entry.getValue();
            Map<String, CrowdinTranslationKeyItemObject> currentCrowdinTranslationKeys = crowdinTranslationKeys.get(entry.getKey());
            unConcurrentExecutorService.submit(Lang.wrapCheckedException(() -> {
                for (Map.Entry<String, DifferenceType> diffItem : currentDifferences.entrySet()) {
                    if (diffItem.getValue().isAllowConcurrency()) {
                        continue;
                    }

                    switch (diffItem.getValue()) {
                        case ADD -> {
                            CrowdinTranslationKeyItemObject key = CrowdinAPI.addTranslationKey(token, project, file, diffItem.getKey(), sourceLanguage.getTranslationValue(diffItem.getKey()));
                            currentCrowdinTranslationKeys.put(key.getIdentifier(), key);
                            for (AbstractI18NFile targetLanguage : targetLanguages) {
                                CrowdinAPI.addTranslationValue(token, project, key, targetLanguage.getLanguage(), targetLanguage.getTranslationValue(diffItem.getKey()));
                            }

                            Logging.getLogger().log(Level.INFO, String.format("+ %s - FINISHED", diffItem.getKey()));
                        }
                        case REMOVE -> {
                            CrowdinAPI.removeTranslationValue(token, project, currentCrowdinTranslationKeys.get(diffItem.getKey()));

                            Logging.getLogger().log(Level.INFO, String.format("- %s - FINISHED", diffItem.getKey()));
                        }
                    }
                }
            }), String.format("Differences Processor #%s", entry.getKey()));
        }

        unConcurrentExecutorService.shutdown();
        while (true) {
            boolean finished = unConcurrentExecutorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
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
