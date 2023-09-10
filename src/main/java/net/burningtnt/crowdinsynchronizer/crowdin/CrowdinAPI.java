package net.burningtnt.crowdinsynchronizer.crowdin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import net.burningtnt.crowdinsynchronizer.crowdin.objects.*;
import net.burningtnt.crowdinsynchronizer.utils.Lang;
import net.burningtnt.crowdinsynchronizer.utils.io.NetIterator;
import net.burningtnt.crowdinsynchronizer.utils.io.NetworkUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class CrowdinAPI {
    private static final Gson GSON = new GsonBuilder().create();

    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

    private CrowdinAPI() {
    }

    public static HttpClient getHttpClient(CrowdinToken token) {
        return HttpClientBuilder.create()
                .setDefaultHeaders(List.of(
                        new BasicHeader("Authorization", "Bearer " + token.getToken())
                ))
                .build();
    }

    public static <T> T parseData(HttpResponse response, Class<T> type) throws IOException {
        return GSON.fromJson(
                GSON.fromJson(
                        NetworkUtils.readResponseBody(response.getEntity()),
                        DataItem.class
                ).getData(),
                type
        );
    }

    public static CrowdinUserDataObject getCurrentUser(CrowdinToken token) throws IOException {
        return parseData(getHttpClient(token).execute(new HttpGet("https://api.crowdin.com/api/v2/user")), CrowdinUserDataObject.class);
    }

    public static NetIterator<CrowdinProjectObject> getProjects(CrowdinToken token, CrowdinUserDataObject user) {
        return new CrowdinPageIterator<>(pageData -> getHttpClient(token).execute(new HttpGet(CrowdinPageIterator.format(
                "https://api.crowdin.com/api/v2/projects",
                Map.of(
                        "userId", String.valueOf(user.getID()),
                        "hasManagerAccess", "1"
                ),
                pageData
        ))), CrowdinProjectObject.class);
    }

    public static NetIterator<CrowdinFileObject> getFiles(CrowdinToken token, CrowdinProjectObject project) {
        return new CrowdinPageIterator<>(pageData -> getHttpClient(token).execute(new HttpGet(CrowdinPageIterator.format(
                String.format("https://api.crowdin.com/api/v2/projects/%s/files", project.getID()),
                Map.of(),
                pageData
        ))), CrowdinFileObject.class);
    }

    public static NetIterator<CrowdinTranslationKeyItemObject> getTranslationKeys(CrowdinToken token, CrowdinProjectObject project, CrowdinFileObject file) {
        return new CrowdinPageIterator<>(pageData -> getHttpClient(token).execute(new HttpGet(CrowdinPageIterator.format(
                String.format("https://api.crowdin.com/api/v2/projects/%s/strings", project.getID()),
                Map.of(
                        "fileId", String.valueOf(file.getID())
                ),
                pageData
        ))), CrowdinTranslationKeyItemObject.class);
    }

    public static CrowdinTranslationKeyItemObject addTranslationKey(CrowdinToken token, CrowdinProjectObject project, CrowdinFileObject file, String key, String sourceText) throws IOException {
        return parseData(getHttpClient(token).execute(Lang.exceptionalTweak(new HttpPost(
                String.format("https://api.crowdin.com/api/v2/projects/%s/strings", project.getID())
        ), httpPost -> {
            httpPost.setHeader("Content-Type", JSON_CONTENT_TYPE);
            httpPost.setEntity(new StringEntity(GSON.toJson(Lang.asJsonObject(Map.of(
                    "text", new JsonPrimitive(sourceText),
                    "identifier", new JsonPrimitive(key),
                    "fileId", new JsonPrimitive(file.getID()),
                    "context", new JsonPrimitive("")
            ))), ContentType.APPLICATION_JSON));
        })), CrowdinTranslationKeyItemObject.class);
    }

    public static void removeTranslationValue(CrowdinToken token, CrowdinProjectObject project, CrowdinTranslationKeyItemObject key) throws IOException {
        getHttpClient(token).execute(new HttpDelete(
                String.format("https://api.crowdin.com/api/v2/projects/%s/strings/%s", project.getID(), key.getID())
        ));
    }

    public static void addTranslationValue(CrowdinToken token, CrowdinProjectObject project, CrowdinTranslationKeyItemObject key, String languageID, String targetText) throws IOException {
        getHttpClient(token).execute(Lang.exceptionalTweak(new HttpPost(
                String.format("https://api.crowdin.com/api/v2/projects/%s/translations", project.getID())
        ), httpPost -> {
            httpPost.setHeader("Content-Type", JSON_CONTENT_TYPE);
            httpPost.setEntity(new StringEntity(GSON.toJson(Lang.asJsonObject(Map.of(
                    "stringId", new JsonPrimitive(key.getID()),
                    "languageId", new JsonPrimitive(languageID),
                    "text", new JsonPrimitive(targetText)
            ))), ContentType.APPLICATION_JSON));
        }));
    }

    public static NetIterator<CrowdinTranslationValueItemObject> getTranslationValue(CrowdinToken token, CrowdinProjectObject project, CrowdinTranslationKeyItemObject key, String languageID) {
        return new CrowdinPageIterator<>(pageData -> getHttpClient(token).execute(new HttpGet(CrowdinPageIterator.format(
                String.format("https://api.crowdin.com/api/v2/projects/%s/translations", project.getID()),
                Map.of(
                        "stringId", String.valueOf(key.getID()),
                        "languageId", languageID
                ),
                pageData
        ))), CrowdinTranslationValueItemObject.class);
    }
}
