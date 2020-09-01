package io.visur.plugins.bgsqlitesync.util;

import com.getcapacitor.JSObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MessageTransformation {
    public static JSONObject getMessageWrapper(JSObject pluginOptions) throws JSONException {
        JSONArray values = pluginOptions.getJSONArray("values");
        JSONObject table = (JSONObject) values.get(0);

        JSONObject wrapper = table.getJSONObject("MessageWrapper");
        return wrapper;
    }
}
