package io.visur.plugins.bgsqlitesync.tobedelete;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import org.json.JSONException;

import java.io.IOException;

public class ToBeDelete {
    private void multiMethod2(PluginCall call, JSArray synchronousMethods, JSArray parallelMethods, String apiUrl, JSObject pluginOptions) throws JSONException, IOException {
        String result = null;
        if (synchronousMethods.length() > 0) {
            for (int i = 0; i < synchronousMethods.length(); i++) {
                switch (synchronousMethods.get(i).toString()) {
                    case "CREATE_OR_ALTER":
                        //result = CREATE_OR_ALTER(call, apiUrl);
                        break;
                    case "FORMSCHEMA_TO_SQLITE":
                        //result = FORMSCHEMA_TO_SQLITE(call, apiUrl,pluginOptions);
                        break;
                    case "MENU_TABLE_RECORDS":
                        //result = MENU_TABLE_RECORDS(call, apiUrl);
                        break;
                    case "RECORDS_TO_SQLITE":
                        //result = RECORDS_TO_SQLITE(call, apiUrl);
                        break;
                    case "SYNC_GRAPHDB_NODE":
                        //result = SYNC_GRAPHDB_NODE(call, apiUrl);
                        break;
                    case "SYNC_SCHEMAS_TO_SQLSERVER":
                        //result = SYNC_SCHEMAS_TO_SQLSERVER(call, apiUrl);
                        break;
                    case "SYNC_RECORDS_TO_SQLSERVER":
                        //result = SYNC_RECORDS_TO_SQLSERVER(call, apiUrl);
                        break;
                    default:
                        break;
                }
            }
            //retResult(call, true, null);
        }
    }

}
