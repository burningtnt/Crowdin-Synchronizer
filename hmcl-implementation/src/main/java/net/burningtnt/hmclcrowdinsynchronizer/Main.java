package net.burningtnt.hmclcrowdinsynchronizer;

import net.burningtnt.crowdinsynchronizer.CrowdinSynchronizer;
import net.burningtnt.crowdinsynchronizer.crowdin.CrowdinToken;
import net.burningtnt.crowdinsynchronizer.locali18n.PropertiesI18NFile;
import net.burningtnt.crowdinsynchronizer.utils.logger.Logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

public final class Main {
    private static final String HMCL_GIT_REPOSITORY = "HMCL-Git-Repository";

    private Main() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String crowdinToken = System.getProperty("hmcl.cs.crowdinToken", "");
        String githubToken = System.getProperty("hmcl.cs.githubToken", "");

        CrowdinSynchronizer.init();

        Path hmclGitPath = Path.of(HMCL_GIT_REPOSITORY).toAbsolutePath();

        if (!Files.exists(hmclGitPath)) {
            Logging.getLogger().log(Level.INFO, "Cloning HMCL git repository.");
            new ProcessBuilder("git", "clone", "-b", "crowdin-translations", String.format("https://%s@github.com/burningtnt/HMCL.git", githubToken), HMCL_GIT_REPOSITORY)
                    .directory(hmclGitPath.getParent().toFile())
                    .inheritIO()
                    .start()
                    .waitFor();
        }

        Path langDir = hmclGitPath.resolve("HMCL/src/main/resources/assets/lang").toAbsolutePath();

        CrowdinSynchronizer.sync(
                CrowdinToken.of(crowdinToken),
                "hello-minecraft-launcher",
                c -> String.format("/I18N-%s.csv", c),
                new PropertiesI18NFile("en", langDir.resolve("I18N.properties")),
                List.of(
                        new PropertiesI18NFile("zh-CN", langDir.resolve("I18N_zh_CN.properties")),
                        new PropertiesI18NFile("zh-TW", langDir.resolve("I18N_zh.properties")),
                        new PropertiesI18NFile("ru", langDir.resolve("I18N_ru.properties")),
                        new PropertiesI18NFile("ja", langDir.resolve("I18N_ja.properties")),
                        new PropertiesI18NFile("es-ES", langDir.resolve("I18N_es.properties"))
                )
        );

        Logging.getLogger().log(Level.INFO, "Pushing HMCL git repository ...");
        new ProcessBuilder("git", "add", ".")
                .directory(hmclGitPath.toFile())
                .inheritIO()
                .start()
                .waitFor();

        new ProcessBuilder("git", "commit", "-m", "[Crowdin Synchronizer] Sync I18N from crowdin.")
                .directory(hmclGitPath.toFile())
                .inheritIO()
                .start()
                .waitFor();

        new ProcessBuilder("git", "push", "origin", "crowdin-translations")
                .directory(hmclGitPath.toFile())
                .inheritIO()
                .start()
                .waitFor();
    }
}
