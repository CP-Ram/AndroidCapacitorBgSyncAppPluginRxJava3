package io.visur.plugins.bgsqlitesync.sqlitedb;

import android.content.Context;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import io.visur.plugins.bgsqlitesync.cdssUtils.GlobalSQLite;
import io.visur.plugins.bgsqlitesync.cdssUtils.SQLiteDatabaseHelper;

public class SqliteDB {
    private GlobalSQLite globalData = new GlobalSQLite();
    private Context context;
    private SQLiteDatabaseHelper mDb;
    private  Boolean encrypted =false;
    private  String inMode = "no-encryption";
    private  String dbName =null;
    public SqliteDB( Context context,Boolean encrypted,String inMode,String dbName) {
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
    public void close(String dbName,PluginCall call) {
        JSObject ret = new JSObject();

        if (dbName == null) {
            retResult(call,false,"Close command failed: Must provide a database name");
            return;
        }
        boolean res = mDb.closeDB(dbName+"SQLite.db");
        mDb = null;
        if (res) {
            retResult(call,true,null);
        } else {
            retResult(call,false,"Close command failed");
        }
    }

    public void execute(String statements,PluginCall call) {
        JSObject retRes = new JSObject();
        retRes.put("changes",Integer.valueOf(-1));
        if (statements == null) {
            retChanges(call,retRes,"Execute command failed : Must provide raw SQL statements");
            call.reject("Must provide raw SQL statements");
            return;
        }
        // split for each statement
        String[] sqlCmdArray = statements.split(";\n");
        // split for a single statement on multilines
        for (int i = 0; i < sqlCmdArray.length; i++) {
            String[] array = sqlCmdArray[i].split("\n");
            StringBuilder builder = new StringBuilder();
            for(String s : array) {
                builder.append(s.trim());
            }
            sqlCmdArray[i] = builder.toString();
        }
        if(sqlCmdArray[sqlCmdArray.length-1] == "") {
            sqlCmdArray = Arrays.copyOf(sqlCmdArray, sqlCmdArray.length-1);
        }
        JSObject res = mDb.execSQL(sqlCmdArray);
        if (res.getInteger("changes") == Integer.valueOf(-1)) {
            retChanges(call, retRes, "Execute command failed");
        } else {
            retChanges(call, res, null);
        }
    }

    public JSObject customExecute(String statements,PluginCall call){
        if(mDb==null)
        {
            open(call);
        }
        JSObject retRes = new JSObject();
        retRes.put("changes",Integer.valueOf(-1));
        if (statements == null) {
            retChanges(call,retRes,"Execute command failed : Must provide raw SQL statements");
            call.reject("Must provide raw SQL statements");
            return retRes;
        }
        // split for each statement
        String[] sqlCmdArray = statements.split(";\n");
        // split for a single statement on multilines
        for (int i = 0; i < sqlCmdArray.length; i++) {
            String[] array = sqlCmdArray[i].split("\n");
            StringBuilder builder = new StringBuilder();
            for(String s : array) {
                builder.append(s.trim());
            }
            sqlCmdArray[i] = builder.toString();
        }
        if(sqlCmdArray[sqlCmdArray.length-1] == "") {
            sqlCmdArray = Arrays.copyOf(sqlCmdArray, sqlCmdArray.length-1);
        }
        JSObject res = mDb.execSQL(sqlCmdArray);
        if (res.getInteger("changes") == Integer.valueOf(-1)) {
            return retRes;
        } else {
            return res;
        }

    }

    public void run(String statement, JSArray values, PluginCall call) throws JSONException {
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

    public JSObject  customRun(String statement,PluginCall call){
        if(mDb==null)
        {
            open(call);
        }
        JSObject res=mDb.runSQL(statement,null);
        JSObject ret = new JSObject();
        ret.put("values", res);
        return ret;
    }

    public void query(String statement,JSArray values, PluginCall call) throws JSONException {
        if (statement == null) {
            retValues(call, new JSArray(), "Must provide a query statement");
            return;
        }
        if(values == null) {
            retValues(call, new JSArray(), "Must provide an Array of values");
            return;
        }
        JSArray res;
        if(values.length() > 0) {
            ArrayList<String> vals = new ArrayList<String>();
            for (int i = 0; i < values.length(); i++) {
                vals.add(values.getString(i));
            }
            res = mDb.querySQL(statement, vals);
        } else {
            res = mDb.querySQL(statement,new ArrayList<String>());
        }

        if (res.length() > 0) {
            retValues(call, res, null);
        } else {
            retValues(call, res, "Query command failed");
        }
    }

    public JSObject  customQuery(String statement,JSArray values,PluginCall call) throws JSONException {
        JSObject ret=null;
        JSArray res;
        if(mDb==null)
        {
            open(call);
        }

        if (statement == null) {
            retValues(call, new JSArray(), "Must provide a query statement");
            return ret;
        }
        if(values == null) {
            retValues(call, new JSArray(), "Must provide an Array of values");
            return ret;
        }

        if(values.length() > 0) {
            ArrayList<String> vals = new ArrayList<String>();
            for (int i = 0; i < values.length(); i++) {
                vals.add(values.getString(i));
            }
            res = mDb.querySQL(statement, vals);
        } else {
            res = mDb.querySQL(statement,new ArrayList<String>());
        }

        ret = new JSObject();
        ret.put("values", res);
        return ret;
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


    public void executeBatchSQLiteQuery(List<String> queries,PluginCall call){
        if(queries.size() >0){
            this.open(call);
         //List<List<String>> tempQueries = this.partition(queries, 10);

            for(String query :queries) {
                this.execute(query,call);
            }
        }
    }


    public static  String createDynamicQuery(JSONObject record,String tableName) throws JSONException {
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
        while(keys.hasNext()){
            String key = keys.next();
            if( record.get(key) != null && record.get(key) instanceof String) {
                 //record.get(key).replace(/\'/g, '\'\'') ;
            }
            builder.append("'").append(record.get(key)).append("'").append(",");
        }
        builder.deleteCharAt(builder.length() - 1);

        builder.append(");");

        return builder.toString();
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

    private static <T> List<List<T>> partition(Collection<T> members, int maxSize)
    {
        List<List<T>> res = new ArrayList<>();

        List<T> internal = new ArrayList<>();

        for (T member : members)
        {
            internal.add(member);

            if (internal.size() == maxSize)
            {
                res.add(internal);
                internal = new ArrayList<>();
            }
        }
        if (internal.isEmpty() == false)
        {
            res.add(internal);
        }
        return res;
    }

}
