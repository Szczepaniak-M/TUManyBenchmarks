import {Injectable} from "@angular/core";
import * as duckdb from '@duckdb/duckdb-wasm';
import {forkJoin} from "rxjs";
import {Filter} from "../list-filter/list-filter.model";

@Injectable({
  providedIn: "root"
})
export class ListQueryService {

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
      this.conn.query(`ALTER TABLE instances ALTER benchmarks TYPE JSON[];`);
    });
  }

  public executeQuery(query: string): Promise<any> {
    return this.conn.query(query)
      .then(response => {
          return {
            columns: response.schema.fields.map(field => field.name),
            rows: JSON.parse(JSON.stringify(response.toArray()))
          }
        },
        error => {
          return {error: error.toString()?.split("\n")}
        });
  }

  public transformFilterToQuery(filter: Filter): string {
    let query = "SELECT i.name as Name, i.vcpu as vCPUs, i.memory as Memory, i.network as Network,";
    if (filter.benchmark) {
      query += " s.min as Minimum, s.avg as Average, s.median as Median, s.max as Maximum,";
    }
    query += " i.tags as Tags\n";
    query += "FROM instances i";
    if (filter.benchmark) {
      query += `, statistics s`;
    }
    query += "\n";
    if (filter.name || filter.minCpu || filter.maxCpu || filter.minMemory || filter.maxMemory
      || filter.network || filter.tags || filter.benchmark) {
      query += "WHERE ";
      const conditions = [];
      if (filter.name) {
        conditions.push(`i.name LIKE '%${filter.name.toLowerCase()}%'`);
      }
      if (filter.minCpu) {
        conditions.push(`i.vcpu >= ${filter.minCpu}`);
      }
      if (filter.maxCpu) {
        conditions.push(`i.vcpu <= ${filter.maxCpu}`);
      }
      if (filter.minMemory) {
        conditions.push(`i.memory >= ${filter.minMemory}`);
      }
      if (filter.maxMemory) {
        conditions.push(`i.memory <= ${filter.maxMemory}`);
      }
      if (filter.network) {
        const networks = filter.network.map(network => `'${network}'`);
        conditions.push(`i.network IN (${networks.join(" ,")})`);
      }
      if (filter.benchmark) {
        const benchmarkSplit = filter.benchmark.split("-");
        const benchmark = benchmarkSplit[0];
        const series = benchmarkSplit[1];
        conditions.push(`i.id = s.instanceId AND s.benchmarkId = '${benchmark}' AND s.series = '${series}'`);
      }
      if (filter.tags) {
        for (const tag of filter.tags) {
          conditions.push(`array_contains(i.tags, '${tag}')`);
        }
      }
      query += conditions.join(" AND ");
    }
    return query;
  }

  private async loadTable(name: string) {
    const jsonData = localStorage.getItem(name);
    const jsonRowContent = jsonData ? JSON.parse(jsonData) : [];
    return this.db.registerFileText(
      `${name}.json`,
      JSON.stringify(jsonRowContent),
    ).then(() =>
      this.conn.insertJSONFromPath(`${name}.json`, {name: name})
    )
  }
}
