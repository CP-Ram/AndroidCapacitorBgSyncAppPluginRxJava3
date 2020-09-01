package io.visur.plugins.bgsqlitesync;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.jayway.jsonpath.JsonPath;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
//import org.json.simple.parser.JSONParser;
//import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.visur.plugins.bgsqlitesync.cdssUtils.GlobalSQLite;
import io.visur.plugins.bgsqlitesync.cdssUtils.SQLiteDatabaseHelper;
import io.visur.plugins.bgsqlitesync.restApi.PullBackendData;
import io.visur.plugins.bgsqlitesync.services.SynchroniztionService;
import io.visur.plugins.bgsqlitesync.sqlitedb.SqliteDB;

@NativePlugin
public class VisurBackgroundSqliteSync extends Plugin {
    private static final String TAG = "VisurBackgroundSqliteSync";
    private static final String DEFAULT_RETURN = "Success";

    SynchroniztionService synchroniztionService=null;

    private Context context;

    public void load() {
        // Get singleton instance of database
        context = getContext();
 /*
        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                e = e.getCause();
            }
            if ((e instanceof IOException) || (e instanceof SocketException)) {
                // fine, irrelevant network problem or API that throws on cancellation
                return;
            }
            if (e instanceof InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return;
            }
            if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
                // that's likely a bug in the application
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                return;
            }
            if (e instanceof IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                Thread.currentThread().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), e);
                return;
            }
            System.out.println(e);
            System.out.println("Undeliverable exception received, not sure what to do"+e);
        });
    */
    }

    @PluginMethod()
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", value);
        call.success(ret);
    }

    @PluginMethod()
    public void syncNow(PluginCall call) throws IOException, JSONException {
        String result = null;

        String apiUrl = call.getString("apiUrl");
        JSArray parallelMethods = call.getArray("parallelMethods");
        JSArray synchronousMethods = call.getArray("synchronousMethods");
        String operationType = call.getString("operationType");
        JSObject pluginOptions = call.getObject("pluginOptions");

        synchroniztionService = new SynchroniztionService(context);

        synchroniztionService.startSyncNow(call, synchronousMethods, parallelMethods, apiUrl, pluginOptions)
                .observeOn(AndroidSchedulers.mainThread()).subscribe(
                res -> {
                    System.out.println(res);
                    retResult(call, true, DEFAULT_RETURN);
                },
                onError -> {
                    System.out.println(onError);
                },
                () -> {
                    System.out.println("Completed!!");
                }
        );
    }


    private void retResult(PluginCall call, Boolean res, String message) {
        JSObject ret = new JSObject();
        ret.put("result", res);
        if (message != null) ret.put("message", message);
        call.resolve(ret);
    }
}
