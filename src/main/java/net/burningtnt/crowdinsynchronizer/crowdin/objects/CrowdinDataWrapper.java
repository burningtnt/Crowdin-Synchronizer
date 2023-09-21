package net.burningtnt.crowdinsynchronizer.crowdin.objects;

import com.google.gson.JsonElement;

public final class CrowdinDataWrapper {
    private final JsonElement data;

    private CrowdinDataWrapper(JsonElement data) {
        this.data = data;
    }

    public JsonElement getData() {
        return this.data;
    }
}
