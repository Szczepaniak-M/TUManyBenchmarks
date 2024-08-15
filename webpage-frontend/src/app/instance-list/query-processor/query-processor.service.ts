import {Injectable} from "@angular/core";
import * as duckdb from '@duckdb/duckdb-wasm';
import {forkJoin} from "rxjs";

@Injectable({
  providedIn: "root"
})
export class QueryProcessorService {

  private db!: duckdb.AsyncDuckDB;
  private conn!: duckdb.AsyncDuckDBConnection;

  public async initializeDuckDB(): Promise<void> {
    const jsDelivrBundles = duckdb.getJsDelivrBundles();
    const bundle = await duckdb.selectBundle(jsDelivrBundles);
    const worker_url = URL.createObjectURL(
      new Blob([`importScripts("${bundle.mainWorker!}");`], {type: 'text/javascript'})
    );
    const worker = new Worker(worker_url);
    const logger = new duckdb.ConsoleLogger();
    this.db = new duckdb.AsyncDuckDB(logger, worker);
    await this.db.instantiate(bundle.mainModule, bundle.pthreadWorker);
    this.conn = await this.db.connect();
    URL.revokeObjectURL(worker_url);
  }

  public loadDatabase() {
    forkJoin([
      this.loadTable('instances'),
      this.loadTable('benchmarks'),
      this.loadTable('statistics')
    ]).subscribe(() => {
      this.conn.query(`ALTER TABLE instances ALTER benchmarks TYPE JSON[];`).then()
    });
    // const x = this.conn.query(`SELECT * FROM statistics WHERE series = 'firstRoundTripClient'`);
  }

  private async loadTable(name: string) {
    const jsonData = localStorage.getItem(name);
    const jsonRowContent = jsonData ? JSON.parse(jsonData) : [];
    return this.db.registerFileText(
      `${name}.json`,
      JSON.stringify(jsonRowContent),
    ).then(() =>
      this.conn.insertJSONFromPath(`${name}.json`, { name: name })
    )
  }
}
