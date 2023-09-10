package net.burningtnt.crowdinsynchronizer.crowdin.objects;

import com.google.gson.JsonElement;

public final class DataItem {
    private final JsonElement data;

    private DataItem(JsonElement data) {
        this.data = data;
    }

    public JsonElement getData() {
        return this.data;
    }
}
