import {Injectable} from "@angular/core";
import * as duckdb from '@duckdb/duckdb-wasm';
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
    this.createTables()
      .then(() => {
        this.loadTable('instances')
        this.loadTable('benchmarks')
        this.loadTable('statistics')
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
    let query = "SELECT i.name as Name, i.on_demand_price as \"On-Demand Price [$/h]\", " +
      "i.spot_price as \"Spot Price [$/h]\", i.vcpu as vCPUs, i.memory as Memory, i.network as Network,";
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
      || filter.network || filter.tagsAll || filter.tagsAny || filter.benchmark) {
      query += "WHERE ";
      const conditions = [];
      if (filter.name) {
        conditions.push(`i.name LIKE '%${filter.name.toLowerCase()}%'`);
      }
      if (filter.minOnDemandPrice) {
        conditions.push(`i.on_demand_price >= ${filter.minOnDemandPrice}`);
      }
      if (filter.maxOnDemandPrice) {
        conditions.push(`i.on_demand_price <= ${filter.maxOnDemandPrice}`);
      }
      if (filter.minSpotPrice) {
        conditions.push(`i.spot_price >= ${filter.minSpotPrice}`);
      }
      if (filter.maxSpotPrice) {
        conditions.push(`i.spot_price <= ${filter.maxSpotPrice}`);
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
        conditions.push(`i.id = s.instance_id AND s.benchmark_id = '${benchmark}' AND s.series = '${series}'`);
      }
      if (filter.tagsAll) {
        const tags = filter.tagsAll.map(tag => `'${tag}'`).join(", ")
        conditions.push(`array_has_all(i.tags, [${tags}])`);
      }
      if (filter.tagsAny) {
        const tags = filter.tagsAny.map(tag => `'${tag}'`).join(", ")
        conditions.push(`array_has_any(i.tags, [${tags}])`);
      }
      query += conditions.join(" AND ");
    }
    return query;
  }

  private async createTables() {
    // rows name must be in alphabetical order for correct import of JSON data
    const creteInstance = `CREATE TABLE "instances" (
      "benchmarks" JSON[],
      "id" VARCHAR PRIMARY KEY NOT NULL,
      "memory" DOUBLE NOT NULL,
      "name" VARCHAR NOT NULL,
      "network" VARCHAR NOT NULL,
      "on_demand_price" DOUBLE NOT NULL,
      "spot_price" DOUBLE NOT NULL,
      "tags" VARCHAR[],
      "vcpu" INTEGER NOT NULL,
    );`

    const createBenchmarks = `CREATE TABLE "benchmarks" (
      "description" VARCHAR NOT NULL,
      "id" VARCHAR PRIMARY KEY NOT NULL,
      "instance_tags" VARCHAR[][],
      "instance_types" VARCHAR[],
      "name" VARCHAR NOT NULL,
      "series_other" VARCHAR[],
      "series_x" VARCHAR[],
      "series_y" VARCHAR[],
    );`

    const createStatistics = `CREATE TABLE "statistics" (
      "avg" DOUBLE,
      "benchmark_id" VARCHAR NOT NULL,
      "instance_id" VARCHAR NOT NULL,
      "max" DOUBLE,
      "median" DOUBLE,
      "min" DOUBLE,
      "series" VARCHAR NOT NULL,
      PRIMARY KEY ("series", "benchmark_id", "instance_id"),
      FOREIGN KEY ("instance_id") REFERENCES "instances" ("id"),
      FOREIGN KEY ("benchmark_id") REFERENCES "benchmarks" ("id")
    );`

    return this.conn.query(creteInstance)
      .then(() => this.conn.query(createBenchmarks))
      .then(() => this.conn.query(createStatistics))
  }

  private async loadTable(name: string) {
    const jsonData = localStorage.getItem(name);
    const jsonRowContent = jsonData ? JSON.parse(jsonData) : [];
    return this.db.registerFileText(
      `${name}.json`,
      JSON.stringify(jsonRowContent),
    ).then(() =>
      this.conn.insertJSONFromPath(`${name}.json`, {name: name, create: false})
    )
  }
}
