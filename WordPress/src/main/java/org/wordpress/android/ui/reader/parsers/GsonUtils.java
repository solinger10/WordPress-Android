package org.wordpress.android.ui.reader.parsers;

import android.text.TextUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.wordpress.android.util.HtmlUtils;

/**
 * GSON helper routines
 */
public class GsonUtils {

    private static final String EMPTY_STRING = "";

    public static String getString(JsonObject json, String name) {
        JsonElement element = json.get(name);
        return (element != null ? element.getAsString(): EMPTY_STRING);
    }

    public static String getStringDecoded(JsonObject json, String name) {
        JsonElement element = json.get(name);
        return (element != null ? HtmlUtils.fastUnescapeHtml(element.getAsString()) : EMPTY_STRING);
    }

    public static String getStringStripHtml(JsonObject json, String name) {
        JsonElement element = json.get(name);
        return (element != null ? HtmlUtils.fastStripHtml(element.getAsString()) : EMPTY_STRING);
    }

    public static long getLong(JsonObject json, String name) {
        JsonElement element = json.get(name);
        return (element != null ? element.getAsLong(): 0);
    }

    public static int getInt(JsonObject json, String name) {
        JsonElement element = json.get(name);
        return (element != null ? element.getAsInt(): 0);
    }

    public static boolean getBool(JsonObject json, String name) {
        JsonElement element = json.get(name);
        return (element != null ? element.getAsBoolean(): false);
    }

    public static JsonObject getChild(final JsonObject jsonParent, final String query) {
        if (jsonParent == null || TextUtils.isEmpty(query)) {
            return null;
        }
        String[] names = query.split("/");
        JsonObject jsonChild = null;
        for (int i = 0; i < names.length; i++) {
            if (jsonChild == null) {
                jsonChild = jsonParent.getAsJsonObject(names[i]);
            } else {
                jsonChild = jsonChild.getAsJsonObject(names[i]);
            }
            if (jsonChild == null) {
                return null;
            }
        }
        return jsonChild;
    }
}
