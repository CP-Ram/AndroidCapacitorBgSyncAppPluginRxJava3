import { WebPlugin } from '@capacitor/core';
import { VisurBackgroundSqliteSyncPlugin, capSQLiteResult, BackendSQLiteSyncOptions } from './definitions';

export class VisurBackgroundSqliteSyncWeb extends WebPlugin implements VisurBackgroundSqliteSyncPlugin {
  constructor() {
    super({
      name: 'VisurBackgroundSqliteSync',
      platforms: ['web'],
    });
  }

  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

  async syncNow(options: BackendSQLiteSyncOptions): Promise<capSQLiteResult> {
    console.log('open', options);
    return Promise.reject("Not implemented");
  }
}

const VisurBackgroundSqliteSync = new VisurBackgroundSqliteSyncWeb();

export { VisurBackgroundSqliteSync };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(VisurBackgroundSqliteSync);
