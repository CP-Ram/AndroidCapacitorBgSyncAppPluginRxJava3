package io.visur.plugins.bgsqlitesync.sqlitedb;

import android.content.Context;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import io.visur.plugins.bgsqlitesync.cdssUtils.GlobalSQLite;
import io.visur.plugins.bgsqlitesync.cdssUtils.SQLiteDatabaseHelper;

public class SqliteDB2 {
    private GlobalSQLite globalData = new GlobalSQLite();
    private Context context;
    private SQLiteDatabaseHelper mDb;
    private  Boolean encrypted =false;
    private  String inMode = "no-encryption";
    private  String dbName =null;

    public SqliteDB2( Context context,Boolean encrypted,String inMode,String dbName) {
        this.context = context;
        this.encrypted = encrypted;
        this.inMode = inMode;
        this.dbName  = dbName;
    }

    public void open(PluginCall call) {
        JSObject ret = new JSObject();
        String secret =null;
        String newsecret = null;

        if (dbName == null) {
            retResult(call,false,"Open command failed: Must provide a database name");
            call.reject("Must provide a database name");
            return;
        }
        if (encrypted) {
            if (!inMode.equals("no-encryption") && !inMode.equals("encryption") &&
                    !inMode.equals("secret") && !inMode.equals("newsecret") &&
                    !inMode.equals("wrongsecret")) {
                retResult(call,false,
                        "Open command failed: Error inMode must be in ['encryption','secret','newsecret']");
            }
            if (inMode.equals("encryption")  || inMode.equals("secret")) {
                secret = globalData.secret;
                // this is only done for testing multiples runs
                newsecret = globalData.newsecret;

            } else if (inMode.equals("newsecret")) {
                secret = globalData.secret;
                newsecret = globalData.newsecret;
            } else if (inMode.equals("wrongsecret")) {
                // for test purpose only
                secret = "wrongsecret";
                inMode = "secret";
            } else {
                secret = "";
                newsecret = "";
            }

        } else {
            inMode = "no-encryption";
            secret = "";
        }
        mDb = new SQLiteDatabaseHelper(context,dbName+"SQLite.db",encrypted,
                inMode,secret,newsecret,1);
        if (!mDb.isOpen) {
            retResult(call,false,"Open command failed: Database " + dbName +
                    "SQLite.db not opened");
        } else {
            retResult(call,true,null);
        }

        //call.resolve(ret);
    }

    public void executeBatchSQLitePreparedStatement(List<String> queryValueJsonString,PluginCall call) throws JSONException {
        if(queryValueJsonString.size() >0){
            this.open(call);
            //List<List<String>> tempQueries = this.partition(queries, 10);

            for(String query :queryValueJsonString) {
                JSONObject queryObject = new JSONObject(query);
                String statement = queryObject.getString("query");
                JSONArray jsonArray = queryObject.getJSONArray("values");
                JSArray values = new JSArray();
                for(int i=0;i<jsonArray.length();i++) {
                    values.put(jsonArray.get(i));
                }
                this.executePreparedStatement(statement,values,call);
            }
        }
    }
    public void executePreparedStatement(String statement,JSArray values,PluginCall call) {
        if(mDb==null)
        {
            open(call);
        }

        JSObject retRes = new JSObject();
        retRes.put("changes",Integer.valueOf(-1));
        if (statement == null) {
            retChanges(call,retRes,
                    "Run command failed: Must provide a SQL statement");
            return;
        }
        if(values == null) {
            retChanges(call,retRes,
                    "Run command failed: Must provide an Array of values");
            return;
        }

        JSObject res;
        if(values.length() > 0) {
            res = mDb.runSQL(statement,values);
        } else {
            res = mDb.runSQL(statement,null);
        }
        if (res.getInteger("changes") == Integer.valueOf(-1)) {
            retChanges(call, retRes, "Run command failed");
        } else {
            retChanges(call, res, null);
        }


    }


    public static  String createPreparedStmtQuery(JSONObject record,String tableName) throws JSONException {
        record = modifyRecord(record);

        Iterator<String> keys = record.keys();
        StringBuilder builder = new StringBuilder();
        builder.append("insert or replace into ");
        builder.append(tableName);
        builder.append(" (");

        while(keys.hasNext()){
            builder.append(keys.next()).append(",");
        }
        builder.setLength(builder.length() - 1);

        builder.append(") values(");
        keys = record.keys();
        JSArray valuesArray = new JSArray();
        while(keys.hasNext()){
            String key = keys.next();
            if( record.get(key) != null && record.get(key) instanceof String) {
                //record.get(key).replace(/\'/g, '\'\'') ;
            }
//            builder.append("'").append(record.get(key)).append("'").append(",");
            builder.append("?").append(",");

            valuesArray.put(record.get(key));
        }
        builder.deleteCharAt(builder.length() - 1);

        builder.append(");");

        JSONObject ret = new JSONObject();
        ret.put("query",builder.toString());
        ret.put("values",valuesArray);
        return ret.toString();
    }
    private static JSONObject modifyRecord(JSONObject record) throws JSONException {

        if (record.has("ValidFrom")) {
            record.remove("ValidFrom");
        }
        if (record.has("ValidTo")) {
            record.remove("ValidTo");
        }

        Iterator<String> keys = record.keys();
        while(keys.hasNext()){
            String key = keys.next();
            if (record.get(key) instanceof JSONObject || record.get(key) instanceof List) {
                record.put(key,((JSObject)record.get(key)).toString());
            }
        }

        return record;
    }

    private void retResult(PluginCall call, Boolean res, String message) {
        JSObject ret = new JSObject();
        ret.put("result", res);
        if(message != null) ret.put("message",message);
        // call.resolve(ret);
    }
    private void retChanges(PluginCall call, JSObject res, String message) {
        JSObject ret = new JSObject();
        ret.put("changes", res);
        if(message != null) ret.put("message",message);
        //  call.resolve(ret);
    }
    private void retValues(PluginCall call, JSArray res, String message) {
        JSObject ret = new JSObject();
        ret.put("values", res);
        if(message != null) ret.put("message",message);
        // call.resolve(ret);
    }

}
