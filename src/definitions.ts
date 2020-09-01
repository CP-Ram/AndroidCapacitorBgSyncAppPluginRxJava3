declare module '@capacitor/core' {
  interface PluginRegistry {
    VisurBackgroundSqliteSync: VisurBackgroundSqliteSyncPlugin;
  }
}

export interface VisurBackgroundSqliteSyncPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  syncNow(options: BackendSQLiteSyncOptions): Promise<capSQLiteResult>;
}

export interface BackendSQLiteSyncOptions {
  apiUrl?:string;
  synchronousMethods?:Array<string>;
  parallelMethods?:Array<string>;
  operationType?:string;
  pluginOptions:capSQLiteOptions
}

export interface capSQLiteOptions {
  /**
   * The database name
   */
  database?: string;
  
  /**
   * The batch of raw SQL statements as string
   */
  statements?: string;
  /**
   * The batch of raw SQL statements as Array of capSQLLiteSet
   */
  set?: Array<capSQLiteSet>;
  /**
   * A statement
   */
  statement?: string;
  /**
   * A set of values for a statement
   */
  values?: Array<any>;
  /**
   * Set to true for database encryption
   */
  encrypted?: boolean;  
  /***
   * Set the mode for database encryption
   * ["encryption", "secret","newsecret"]
   */
  mode?: string;
  /***
   * Set the JSON object to import
   *
   */
  jsonstring?: string;
  /***
   * Set the mode to export JSON Object
   * "full", "partial"
   *
   */
  jsonexportmode?: string; 
  /***
   * Set the synchronization date
   *
   */
  syncdate?: string; 

}

export interface capSQLiteResult {
  /**
   * result set to true when successful else false
   */
  result?: boolean;
  /**
   * the number of changes from an execute or run command
   */
  changes?: any;
  /**
   * the data values list as an Array
   */
  values?: Array<any>;
  /**
   * a message
   */
  message?: string;
  /**
   * an export JSON object
   */
  export?: any
}
export interface capSQLiteSet {
  /**
   * A statement
   */
  statement?: String;
  /**
   * the data values list as an Array
   */
  values?: Array<any>;
}

