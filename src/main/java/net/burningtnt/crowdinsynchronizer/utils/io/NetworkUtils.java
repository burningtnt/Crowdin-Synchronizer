package net.burningtnt.crowdinsynchronizer.utils.io;

import org.apache.http.HttpEntity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class NetworkUtils {
    private NetworkUtils() {
    }

    public static String encodeURL(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    public static URI format(String url, Map<String, String> queryArgs) throws IOException {
        try {
            if (queryArgs.size() == 0) {
                return new URI(url);
            }
            return format(url, queryArgs.entrySet().stream());
        } catch (URISyntaxException exception) {
            throw new IOException("Cannot encode URI.", exception);
        }
    }

    public static URI format(String url, Stream<Map.Entry<String, String>> queryArgs) throws IOException {
        try {
            return new URI(url + "?" + queryArgs.map(
                    entry -> encodeURL(entry.getKey()) + "=" + encodeURL(entry.getValue())
            ).collect(Collectors.joining("&")));
        } catch (URISyntaxException exception) {
            throw new IOException("Cannot encode URI.", exception);
        }
    }

    public static String getContentEncoding(HttpEntity httpEntity) {
        if (httpEntity.getContentEncoding() == null) {
            return StandardCharsets.UTF_8.name();
        }
        return httpEntity.getContentEncoding().getValue();
    }

    public static InputStreamReader readResponseBody(HttpEntity httpEntity) throws IOException {
        return new InputStreamReader(httpEntity.getContent(), NetworkUtils.getContentEncoding(httpEntity));
    }
}
