package io.visur.plugins.bgsqlitesync.services;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.jayway.jsonpath.JsonPath;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.visur.plugins.bgsqlitesync.cdssUtils.GlobalSQLite;
import io.visur.plugins.bgsqlitesync.cdssUtils.SQLiteDatabaseHelper;
import io.visur.plugins.bgsqlitesync.restApi.PullBackendData;
import io.visur.plugins.bgsqlitesync.sqlitedb.SqliteDB;
import io.visur.plugins.bgsqlitesync.sqlitedb.SqliteDB2;
import io.visur.plugins.bgsqlitesync.util.MessageTransformation;

public class SynchroniztionService {

    private static final String DEFAULT_RETURN = "Success";
    private GlobalSQLite globalData = new GlobalSQLite();
    private String defaultSQLiteDBName = "VisurConstructionDB";

    private SQLiteDatabaseHelper mDb;
    private PullBackendData rest = new PullBackendData();
    private Context context;

    public SynchroniztionService( Context context){
        this.context=context;
    }

    // ###### ##Sync Now

    public @NonNull Observable<String> startSyncNow(PluginCall call, JSArray synchronousMethods, JSArray parallelMethods, String apiUrl, JSObject pluginOptions) throws JSONException, IOException {

        return createOrAlterTable(call, apiUrl, pluginOptions)
                .switchMap(d -> formSchemaToSQLite(call, apiUrl, pluginOptions))
                .observeOn(Schedulers.io())
                .switchMap(d -> menuTableRecords(call, apiUrl,pluginOptions))
                .switchMap(d -> recordsToSQLite(call, apiUrl,pluginOptions))
                .switchMap(d -> syncGraphDBNode(call, apiUrl,pluginOptions))
                .switchMap(d -> syncSchemasToSQLServer(call, apiUrl,pluginOptions))
                .switchMap(d -> syncRecordsToSQLServer(call, apiUrl,pluginOptions))
                .map(d->d);
    }

    //### createOrAlterTable Start

    private Observable<JSONArray> createOrAlterTable(PluginCall call, String apiUrl, JSObject pluginOptions) throws IOException, JSONException {
        //Read table from backend
        //Read last sync file
        //get user data
        return Observable.zip(readTableFromBackend(pluginOptions, apiUrl), readLastSyncFile(call, pluginOptions), getUserData(call,pluginOptions), (s1, s2, s3) -> readTableFromBackendZipperFun(s1, s2, s3,call,pluginOptions)).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map(d -> d);

       /* return Observable.zip(READ_TABLE_FROM_BACKEND(),READ_LAST_SYNC_FILE(),GET_USER_DATA())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(val->{
                    System.out.println("zip operator test");
                 });*/
        //return DEFAULT_RETURN;
    }
    private @NonNull Observable<String> readTableFromBackend(JSObject pluginOptions, String apiUrl) throws JSONException, IOException {
        JSONObject obj = null;

        JSONObject msgPayload = new JSONObject();
        JSONObject wrapper = MessageTransformation.getMessageWrapper(pluginOptions);

        wrapper.put("Payload", msgPayload.toString());
        wrapper.put("DataType", "SQLLiteDDLAsync");
        wrapper.put("MessageKind", "READ");


        // let data = JSON.parse(JSON.stringify(message));
        // let options: any = getHttpHeaderOptions();
        String response = this.rest.post(apiUrl, wrapper.toString());
        String jsonFormattedString = new JSONTokener(response).nextValue().toString();
        // return this.http.post(apiUrl, wrapper, options)
        //       .pipe(map((response: any) => { return response; }));

        //obj = new JSONObject(jsonFormattedString);
        return Observable.fromArray(jsonFormattedString);
    }
    private @NonNull Observable<JSONObject> readLastSyncFile(PluginCall call, JSObject pluginOptions) throws JSONException {
        JSONObject obj = new JSONObject();
        String dbName = pluginOptions.getString("database");
        boolean encrypted = pluginOptions.getBoolean("encrypted");
        String inmode = pluginOptions.getString("mode");
        SqliteDB sqlite = new SqliteDB(this.context, encrypted, inmode, dbName);

        String query = getLastSyncFileQuery(null, null);
        JSArray jsArray = new JSArray();
        obj = sqlite.customQuery(query,new JSArray(),call);
        return Observable.fromArray(obj);
    }
    private String getLastSyncFileQuery(String table, String operationType) {
        String query = null;
        if (operationType != null) {
            query = "select lastfileversion from LastSync where operationtype='" + operationType + "';";
        } else {
            query = "select lastfileversion from LastSync;";
        }
        return query;
    }

    private @NonNull Observable<Boolean> getUserData(PluginCall call,JSObject pluginOptions) throws JSONException {
        boolean result = false;
        //SELECT name FROM sqlite_master WHERE type='table' AND name='{table_name}';
        String query = "SELECT name FROM sqlite_master where type='table' and name='User';";

        String dbName = pluginOptions.getString("database");
        boolean encrypted = pluginOptions.getBoolean("encrypted");
        String inmode = pluginOptions.getString("mode");
        SqliteDB sqlite = new SqliteDB(this.context, encrypted, inmode, dbName);

        JSONObject obj = new JSONObject();

        obj = sqlite.customQuery(query,new JSArray(),call);
        JSONArray jsonArray = obj.getJSONArray("values");

        if(jsonArray.length()>0){
            result=true;
        }

        return Observable.fromArray(result);
    }

    private JSONArray readTableFromBackendZipperFun(String sqlliteDDL, JSONObject lastFV, boolean userDataFlag, PluginCall call, JSObject pluginOptions) throws JSONException, JsonProcessingException {
        //JSONParser parser = new JSONParser();

        if (lastFV != null && lastFV.has("values")) {
            JSONArray array = (JSONArray) lastFV.get("values");
            String lastfileversion = null;
            if (array.length() > 0) {
                JSONObject jsonObject = (JSONObject) array.get(0);
                lastfileversion = (String) jsonObject.get("lastfileversion");
            }
            JSONObject sqlliteddlObject = new JSONObject(sqlliteDDL);//null;//(JSONObject) parser.parse(sqlliteDDL);
            JSONObject sqlLiteDDl = this.filterSqlLiteDDLBasedOnFileVersion(sqlliteddlObject, lastfileversion, userDataFlag);

            //JSONArray obslist$ = new JSONArray();

            String fileversionName = "";
            JSONArray jsonArray = sqlliteddlObject.names();
            for(int i=0;i<jsonArray.length();i++){
                String element = jsonArray.getString(i);

                String isType = element.toLowerCase().contains("create")?"Create":"Alter";
                JSONObject jObject = (JSONObject) sqlliteddlObject.get(element);
                Object resJsonPathObject = JsonPath.read(jObject.toString(),"$.structure.tables");

                //System.out.println(resJsonPathObject);
                // String jsonPathJSONObject = new JSONTokener(resJsonPathObject.toString()).nextValue().toString();//
                //// https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
                //compile group: 'com.fasterxml.jackson.core', name: 'jackson-databind', version: '2.11.2'
                ObjectMapper mapper = new ObjectMapper();
                String jsonString = mapper.writeValueAsString(resJsonPathObject);

                JSONObject jsonPathJSONObject= new JSONObject(jsonString);

                fileversionName = isType == "Alter" ? element : null;
                JSONObject jsonObj = jsonPathJSONObject!=null?jsonPathJSONObject:new JSONObject();

                //observable
                JSONObject obseravel$ = persistenceCreateOrAlterTable(jsonObj.toString(),isType,pluginOptions,call);

                //obslist$.put(obseravel$);
            }
            if (fileversionName!="") {

                //observable
                JSONObject obseravel$ = fileVersionUpdate(fileversionName,pluginOptions,call);
                //obslist$.put(obseravel$);
            }

            //forkjoin return
        }

        JSONArray jsonArray = new JSONArray();
        return jsonArray;
    }
    private JSONObject fileVersionUpdate(String fileName,JSObject pluginOptions, PluginCall call) throws JSONException {
        String FileVersionUpdatequery = persistenceGetFileVersionUpdatequery(fileName);
        String dbName = pluginOptions.getString("database");
        boolean encrypted = pluginOptions.getBoolean("encrypted");
        String inmode = pluginOptions.getString("mode");
        SqliteDB sqlite = new SqliteDB(this.context, encrypted, inmode, dbName);
        JSONObject res = sqlite.customExecute(FileVersionUpdatequery,call);
        return res;
    }
    private String persistenceGetFileVersionUpdatequery(String fileName) {
        return "Update LastSync Set lastfileversion = '"+fileName+"';";
    }

    private JSONObject filterSqlLiteDDLBasedOnFileVersion(JSONObject sqlliteDDL, String lastfileversion, boolean userDataFlag) throws JSONException {
        JSONObject tempsqlLiteDDl = new JSONObject();
        if (lastfileversion == null && !userDataFlag) {
            Iterator<String> keys = sqlliteDDL.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.contains("Create")) {
                    tempsqlLiteDDl.put(key, sqlliteDDL.get(key));
                }
            }
        } else {
            JSONArray jsonArray = sqlliteDDL.names();

            List<String> sqlLiteDDlList = new ArrayList<String>();
            for(int i=0;i<jsonArray.length();i++){
                sqlLiteDDlList.add(jsonArray.getString(i));
            }

            int fileversionindex = sqlLiteDDlList.indexOf(lastfileversion);

            for(int index=0;index<jsonArray.length();index++){
                String element = jsonArray.getString(index);
                if (element.contains("Create")) {
                    tempsqlLiteDDl.put(element,sqlliteDDL.get(element));//sqlLiteDDl[element];
                } else if (element.contains("Alter") && index > fileversionindex) {
                    tempsqlLiteDDl.put(element,sqlliteDDL.get(element));//sqlLiteDDl[element];
                }
            }

        }
        return tempsqlLiteDDl;
    }
    private JSONObject persistenceCreateOrAlterTable(String querylist, String type, JSObject pluginOptions, PluginCall call) throws JSONException {
        //   JSONParser parser = new JSONParser();

        JSONObject queries = new JSONObject(querylist);//null;//(JSONObject)parser.parse(querylist);

        String statement = getqueryStatement(queries,type);

        String dbName = pluginOptions.getString("database");
        boolean encrypted = pluginOptions.getBoolean("encrypted");
        String inmode = pluginOptions.getString("mode");
        SqliteDB sqlite = new SqliteDB(this.context, encrypted, inmode, dbName);

        JSONObject result = sqlite.customExecute(statement,call);

        return result;
    }
    private String getqueryStatement(JSONObject queries, String type) throws JSONException {

        String statement = "BEGIN TRANSACTION;\n";
        JSONArray tablelist = queries.names();
        for (int i=0;i<tablelist.length();i++){
            String tableName = tablelist.getString(i);
            switch (type){
                case "Create":
                    statement = statement+" CREATE TABLE IF NOT EXISTS "+tableName+" "+queries.get(tableName)+";\n";
                    break;
                case "Alter":
                    statement = statement+" ALTER TABLE "+tableName+" "+queries.get(tableName)+";\n";
                    break;
                default:
                    break;
            }
        }
        statement = statement+" COMMIT TRANSACTION;";
        return tablelist.length()>0 ? statement : null;
    }

    //### createOrAlterTable End


    //### formSchemaToSQLite Start
    private @NonNull Observable<String> formSchemaToSQLite(PluginCall call, String apiUrl, JSObject pluginOptions) throws JSONException, IOException {
        JSONArray values = pluginOptions.getJSONArray("values");

        String dbName = pluginOptions.getString("database");
        boolean encrypted = pluginOptions.getBoolean("encrypted");
        String inmode = pluginOptions.getString("mode");
        SqliteDB2 sqlite = new SqliteDB2(this.context, encrypted, inmode, dbName);

        for (int i = 0; i < values.length(); i++) {
            JSONObject table = (JSONObject) values.get(i);
            String lastSyncTime = table.getString("LastSyncTime");

            String tableName = "FormSchema";//table.getString("tableName");

            JSONObject msgPayload = new JSONObject();
            if(lastSyncTime=="null"){
                msgPayload.put("lastsynctime",JSONObject.NULL);
            }
            else {
                msgPayload.put("lastsynctime", lastSyncTime);
            }
            JSONObject wrapper = table.getJSONObject("MessageWrapper");
            wrapper.put("Payload", msgPayload.toString());
            wrapper.put("DataType", "FormSchemaSync");
            wrapper.put("MessageKind", "READ");

            System.out.println("before rest");
            String response = this.rest.post(apiUrl, wrapper.toString());
            String jsonFormattedString = new JSONTokener(response).nextValue().toString();

            //System.out.println(response);
            JSONArray jsArray = new JSONArray(jsonFormattedString);
            List<String> queries = new ArrayList<String>();
            for (int j = 0; j < jsArray.length(); j++) {
                String query = SqliteDB2.createPreparedStmtQuery(((JSONObject) jsArray.get(j)), tableName);
                queries.add(query);
            }
           // sqlite.executeBatchSQLiteQuery(queries, call);
            sqlite.executeBatchSQLitePreparedStatement(queries,call);
        }

        return Observable.fromArray(DEFAULT_RETURN);
    }
    //### formSchemaToSQLite End


    private @NonNull Observable<String> menuTableRecords(PluginCall call, String apiUrl,JSObject pluginOptions) throws JSONException, IOException {
        JSONObject payload = new JSONObject();

        JSONArray values = pluginOptions.getJSONArray("values");

        String dbName = pluginOptions.getString("database");
        boolean encrypted = pluginOptions.getBoolean("encrypted");
        String inmode = pluginOptions.getString("mode");
        SqliteDB2 sqlite = new SqliteDB2(this.context, encrypted, inmode, dbName);

        for (int i = 0; i < values.length(); i++) {
            JSONObject table = (JSONObject) values.get(i);
            String lastSyncTime = table.getString("LastSyncTime");

            //String tableName = "FormSchema";

            JSONObject msgPayload = new JSONObject();
            if(lastSyncTime=="null"){
                msgPayload.put("lastsynctime",JSONObject.NULL);
            }
            else {
                msgPayload.put("lastsynctime", lastSyncTime);
            }
            JSONObject wrapper = table.getJSONObject("MessageWrapper");
            wrapper.put("Payload", msgPayload.toString());
            wrapper.put("DataType", "MenuTablesSync");
            wrapper.put("MessageKind", "READ");

            //send to backend rest api
            String resString = this.rest.post(apiUrl, wrapper.toString());
            String jsonFormattedString = new JSONTokener(resString).nextValue().toString();

            JSONObject response = new JSONObject(jsonFormattedString);

            //insert sync record
            insertSyncRecordMenuTableAndTreeTable(call,response,sqlite);

            //application event menu sync event

        }




        return Observable.fromArray("MENU_TABLE_RECORDS");
    }

    private void insertSyncRecordMenuTableAndTreeTable(PluginCall call,JSONObject response,SqliteDB2 sqlite) throws JSONException {
        JSONArray treeData=new JSONArray();
        JSONArray keys = response.names();

        for(int i=0;i<keys.length();i++) {
            String key = keys.getString(i);
            String tableName=key;
            //JSONArray queries = new JSONArray();
            JSONArray tempArray = new JSONArray(response.getString(key));
            if (key.equals("RightMenu")) {
                treeData = tempArray;
            }
            List<String> queries = new ArrayList<String>();
            for(int j=0;j<tempArray.length();j++){

                String query = SqliteDB2.createPreparedStmtQuery(((JSONObject) tempArray.get(j)), tableName);
                // this.logger.log('insertSyncRecords$() :: tempArray :: ' + key + ' :: ' + query);
                queries.add(query);
            }

            sqlite.executeBatchSQLitePreparedStatement(queries,call);
        }

        if (treeData.length() > 0) {
            createTreeTables(treeData);
        }


    }

    private void createTreeTables(JSONArray treeData) throws JSONException {

        List<String> sqLitetables=new ArrayList<String>();
        for(int i=0; i<treeData.length(); i++){
            Object tempObj = treeData.get(i);
            JSONObject tempData;
            if(tempObj instanceof String){
                tempData=new JSONObject((String)tempObj);
            }
            else
                tempData=(JSONObject) tempObj;

            if(tempData.getString("LeftMenu")=="LeftNavigation"){
                sqLitetables.add(tempData.getString("RightMenu")+"_Tree");
            }
        }

        JSONObject tableQueries = new JSONObject();

        for(int i=0; i<sqLitetables.size();i++) {
            String tableName = sqLitetables.get(i);
            String table =getTableName(tableName,null);
            //tableQueries.put(table,CreateTreeTableQuery);
        }

        //var statement = this.queryBuilder.getqueryStatement(tableQueries, 'Create');
       // await this.executeSQLiteQuery(statement);
    }

    private String  getTableName(String schemaName ,String accordianName) {
        String tableName = null;

        if(accordianName!=null){
            tableName = schemaName + "_" + accordianName;
        }
        else{
            tableName = schemaName;
        }
        tableName = tableName!=null ? tableName.replace("[^a-zA-Z0-9_]", "") : tableName;
        return tableName.toLowerCase();
    }

    private @NonNull Observable<String> recordsToSQLite(PluginCall call, String apiUrl,JSObject pluginOptions) {
        return Observable.fromArray("RECORDS_TO_SQLITE");
    }

    private @NonNull Observable<String> syncGraphDBNode(PluginCall call, String apiUrl,JSObject pluginOptions) {
        return Observable.fromArray("SYNC_GRAPHDB_NODE");

    }

    private @NonNull Observable<String> syncSchemasToSQLServer(PluginCall call, String apiUrl,JSObject pluginOptions) {
        return Observable.fromArray("SYNC_SCHEMAS_TO_SQLSERVER");

    }

    private @NonNull Observable<String> syncRecordsToSQLServer(PluginCall call, String apiUrl,JSObject pluginOptions) {
        return Observable.fromArray("SYNC_RECORDS_TO_SQLSERVER");
    }


}
