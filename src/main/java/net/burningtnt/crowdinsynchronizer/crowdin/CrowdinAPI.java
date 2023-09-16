package net.burningtnt.crowdinsynchronizer.crowdin;

import com.google.gson.*;
import net.burningtnt.crowdinsynchronizer.crowdin.objects.*;
import net.burningtnt.crowdinsynchronizer.utils.Lang;
import net.burningtnt.crowdinsynchronizer.utils.io.ExceptionalFunction;
import net.burningtnt.crowdinsynchronizer.utils.io.NetIterator;
import net.burningtnt.crowdinsynchronizer.utils.io.NetworkUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CrowdinAPI {
    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

    private static final Map<CrowdinToken, HttpClientDelegate> httpClientDelegates = new ConcurrentHashMap<>();

    public static final class HttpClientDelegate {
        private final HttpClientBuilder builder;

        public HttpClientDelegate(CrowdinToken token) {
            this.builder = HttpClientBuilder.create().setDefaultHeaders(Collections.singletonList(
                    new BasicHeader("Authorization", "Bearer " + token.getToken())
            ));
        }

        public <T> T execute(HttpUriRequest request, ExceptionalFunction<HttpResponse, T, IOException> action) throws IOException {
            try (CloseableHttpClient httpClient = this.builder.build()) {
                HttpResponse response = httpClient.execute(request);
                if (response.getStatusLine().getStatusCode() / 100 != 2) {
                    throw new IOException(String.format("%s[%d]: %s.",
                            response.getStatusLine().getReasonPhrase(), response.getStatusLine().getStatusCode(),
                            Optional.ofNullable(response.getEntity())
                                    .map(httpEntity -> Lang.tryInvoke(() -> Lang.readAllBytesAsString(() -> NetworkUtils.readResponseBody(httpEntity)), null))
                                    .orElse("null")
                    ));
                }
                return action.apply(response);
            }
        }

        public <T> T execute(HttpUriRequest request, Class<T> clazz) throws IOException {
            return this.execute(request, response -> parseData(response, clazz));
        }

        public void execute(HttpUriRequest request) throws IOException {
            this.execute(request, ExceptionalFunction.identity());
        }
    }

    private CrowdinAPI() {
    }

    public static HttpClientDelegate getHttpClient(CrowdinToken token) {
        return httpClientDelegates.computeIfAbsent(token, HttpClientDelegate::new);
    }

    private static <T> T parseData(HttpResponse response, Class<T> type) throws IOException {
        String entity = Lang.readAllBytesAsString(() -> NetworkUtils.readResponseBody(response.getEntity()));

        return Lang.getGson().fromJson(
                Lang.getGson().fromJson(entity, DataItem.class).getData(),
                type
        );
    }

    public static CrowdinUserDataObject getCurrentUser(CrowdinToken token) throws IOException {
        return getHttpClient(token).execute(new HttpGet("https://api.crowdin.com/api/v2/user"), CrowdinUserDataObject.class);
    }

    public static NetIterator<CrowdinProjectObject> getProjects(CrowdinToken token, CrowdinUserDataObject user) {
        return new CrowdinPageIterator<>(pageData -> new HttpGet(CrowdinPageIterator.format(
                "https://api.crowdin.com/api/v2/projects",
                Map.of(
                        "userId", String.valueOf(user.getID()),
                        "hasManagerAccess", "1"
                ),
                pageData
        )), () -> getHttpClient(token), CrowdinProjectObject.class);
    }

    public static NetIterator<CrowdinFileObject> getFiles(CrowdinToken token, CrowdinProjectObject project) {
        return new CrowdinPageIterator<>(pageData -> new HttpGet(CrowdinPageIterator.format(
                String.format("https://api.crowdin.com/api/v2/projects/%s/files", project.getID()),
                Map.of(),
                pageData
        )), () -> getHttpClient(token), CrowdinFileObject.class);
    }

    private static final String[] FILE_DEFAULT_COLUMNS = {"identifier", "sourcePhrase", "context"};

    public static CrowdinFileObject addFile(CrowdinToken token, CrowdinProjectObject project, String filePath, List<String> languages) throws IOException {
        CrowdinStorageObject storage = getHttpClient(token).execute(Lang.tweak(new HttpPost(
                "https://api.crowdin.com/api/v2/storages"
        ), httpPost -> {
            int index = filePath.lastIndexOf("/");
            httpPost.setHeader("Crowdin-API-FileName", index == -1 ? filePath : filePath.substring(index));
            httpPost.setEntity(new StringEntity(",", ContentType.TEXT_PLAIN));
        }), CrowdinStorageObject.class);

        return Lang.exceptionalTweak(
                getHttpClient(token).execute(Lang.tweak(new HttpPost(
                        NetworkUtils.format(
                                String.format("https://api.crowdin.com/api/v2/projects/%s/files", project.getID()),
                                Map.of()
                        )
                ), httpPost -> {
                    httpPost.setHeader("Content-Type", JSON_CONTENT_TYPE);
                    httpPost.setEntity(new StringEntity(Lang.getGson().toJson(Lang.asJsonObject(Map.of(
                            "storageId", new JsonPrimitive(storage.getID()),
                            "name", new JsonPrimitive(filePath),
                            "type", new JsonPrimitive("csv"),
                            "importOptions", Lang.asJsonObject(Map.of(
                                    "scheme", Lang.asJsonObject(Lang.tweak(new TreeMap<>(), map -> {
                                        for (int i = 0; i < FILE_DEFAULT_COLUMNS.length; i++) {
                                            map.put(FILE_DEFAULT_COLUMNS[i], new JsonPrimitive(i));
                                        }
                                        for (int i = 0; i < languages.size(); i++) {
                                            map.put(languages.get(i), new JsonPrimitive(i + FILE_DEFAULT_COLUMNS.length));
                                        }
                                    }))
                            ))
                    ))), ContentType.APPLICATION_JSON));
                }), CrowdinFileObject.class),
                file -> getTranslationKeys(token, project, file).exceptionalForEachRemaining(key -> removeTranslationKey(token, project, key))
        );
    }

    public static void deleteFile(CrowdinToken token, CrowdinProjectObject project, CrowdinFileObject file) throws IOException {
        getHttpClient(token).execute(new HttpDelete(NetworkUtils.format(
                String.format("https://api.crowdin.com/api/v2/projects/%s/files/%s", project.getID(), file.getID()),
                Map.of()
        )));
    }

    public static NetIterator<CrowdinTranslationKeyItemObject> getTranslationKeys(CrowdinToken token, CrowdinProjectObject project, CrowdinFileObject file) {
        return new CrowdinPageIterator<>(pageData -> new HttpGet(CrowdinPageIterator.format(
                String.format("https://api.crowdin.com/api/v2/projects/%s/strings", project.getID()),
                Map.of(
                        "fileId", String.valueOf(file.getID())
                ),
                pageData
        )), () -> getHttpClient(token), CrowdinTranslationKeyItemObject.class);
    }

    public static CrowdinTranslationKeyItemObject addTranslationKey(CrowdinToken token, CrowdinProjectObject project, CrowdinFileObject file, String key, String sourceText) throws IOException {
        return getHttpClient(token).execute(Lang.exceptionalTweak(new HttpPost(
                String.format("https://api.crowdin.com/api/v2/projects/%s/strings", project.getID())
        ), httpPost -> {
            httpPost.setHeader("Content-Type", JSON_CONTENT_TYPE);
            httpPost.setEntity(new StringEntity(Lang.getGson().toJson(Lang.asJsonObject(Map.of(
                    "text", new JsonPrimitive(sourceText),
                    "identifier", new JsonPrimitive(key),
                    "fileId", new JsonPrimitive(file.getID()),
                    "context", new JsonPrimitive("")
            ))), ContentType.APPLICATION_JSON));
        }), CrowdinTranslationKeyItemObject.class);
    }

    public static void removeTranslationKey(CrowdinToken token, CrowdinProjectObject project, CrowdinTranslationKeyItemObject key) throws IOException {
        getHttpClient(token).execute(new HttpDelete(
                String.format("https://api.crowdin.com/api/v2/projects/%s/strings/%s", project.getID(), key.getID())
        ));
    }

    public static void addTranslationValue(CrowdinToken token, CrowdinProjectObject project, CrowdinTranslationKeyItemObject key, String languageID, String targetText) throws IOException {
        getHttpClient(token).execute(Lang.exceptionalTweak(new HttpPost(
                String.format("https://api.crowdin.com/api/v2/projects/%s/translations", project.getID())
        ), httpPost -> {
            httpPost.setHeader("Content-Type", JSON_CONTENT_TYPE);
            httpPost.setEntity(new StringEntity(Lang.getGson().toJson(Lang.asJsonObject(Map.of(
                    "stringId", new JsonPrimitive(key.getID()),
                    "languageId", new JsonPrimitive(languageID),
                    "text", new JsonPrimitive(targetText)
            ))), ContentType.APPLICATION_JSON));
        }));
    }

    public static NetIterator<CrowdinTranslationValueItemObject> getTranslationValue(CrowdinToken token, CrowdinProjectObject project, CrowdinTranslationKeyItemObject key, String languageID) {
        return new CrowdinPageIterator<>(pageData -> new HttpGet(CrowdinPageIterator.format(
                String.format("https://api.crowdin.com/api/v2/projects/%s/translations", project.getID()),
                Map.of(
                        "stringId", String.valueOf(key.getID()),
                        "languageId", languageID
                ),
                pageData
        )), () -> getHttpClient(token), CrowdinTranslationValueItemObject.class);
    }
}
