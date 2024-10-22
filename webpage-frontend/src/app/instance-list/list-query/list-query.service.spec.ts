import {fakeAsync, TestBed, tick} from "@angular/core/testing";
import {ListQueryService} from "./list-query.service";
import * as duckdb from "@duckdb/duckdb-wasm";
import {Filter} from "../list-filter/list-filter.model";
import {MockService} from "ng-mocks";
import {Table} from "apache-arrow";

describe("ListQueryService", () => {
  let service: ListQueryService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ListQueryService],
    });
    service = TestBed.inject(ListQueryService);

  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  it("should load the database tables and alter the instances table", fakeAsync(() => {
    const mockJSONData = [{id: 1, name: "test"}];
    service["db"] = MockService(duckdb.AsyncDuckDB)
    service["conn"] = MockService(duckdb.AsyncDuckDBConnection)
    spyOn(localStorage, "getItem").and.returnValue(JSON.stringify(mockJSONData));
    spyOn(service["db"], "registerFileText").and.returnValue(Promise.resolve());
    spyOn(service["conn"], "query").and.returnValue(Promise.resolve(new Table()));
    spyOn(service["conn"], "insertJSONFromPath").and.returnValue(Promise.resolve());

    service.loadDatabase();
    tick();

    expect(localStorage.getItem).toHaveBeenCalledTimes(3);
    expect(localStorage.getItem).toHaveBeenCalledWith("instances");
    expect(localStorage.getItem).toHaveBeenCalledWith("benchmarks");
    expect(localStorage.getItem).toHaveBeenCalledWith("statistics");

    expect(service["db"].registerFileText).toHaveBeenCalledTimes(3);
    expect(service["db"].registerFileText).toHaveBeenCalledWith("instances.json", JSON.stringify(mockJSONData));
    expect(service["db"].registerFileText).toHaveBeenCalledWith("benchmarks.json", JSON.stringify(mockJSONData));
    expect(service["db"].registerFileText).toHaveBeenCalledWith("statistics.json", JSON.stringify(mockJSONData));

    expect(service["conn"].query).toHaveBeenCalledTimes(3);
    expect(service["conn"].insertJSONFromPath).toHaveBeenCalledWith("instances.json", {name: "instances", create: false});
    expect(service["conn"].insertJSONFromPath).toHaveBeenCalledWith("benchmarks.json", {name: "benchmarks", create: false});
    expect(service["conn"].insertJSONFromPath).toHaveBeenCalledWith("statistics.json", {name: "statistics", create: false});
  }));

  it("should return formatted query results on successful query execution", async () => {
    service["conn"] = MockService(duckdb.AsyncDuckDBConnection)
    const mockResponse = {
      schema: {
        fields: [{name: "column1"}, {name: "column2"}],
      },
      toArray: () => [{column1: "value1", column2: "value2"}],
    };
    // @ts-ignore
    spyOn(service["conn"], "query").and.returnValue(Promise.resolve(mockResponse));

    const result = await service.executeQuery("SELECT * FROM table");

    expect(result.columns).toEqual(["column1", "column2"]);
    expect(result.rows).toEqual([{column1: "value1", column2: "value2"}]);
  });

  it("should parse query errors on failed query execution", async () => {
    service["conn"] = MockService(duckdb.AsyncDuckDBConnection)
    spyOn(service["conn"], "query").and.returnValue(Promise.reject(new Error("Query failed\nInvalid syntax")));

    const result = await service.executeQuery("SELECT * FROM table");

    expect(result.error).toEqual(["Error: Query failed", "Invalid syntax"]);
  });

  it("should transform a Filter object into a SQL query string", () => {
    const filter: Filter = {
      name: "instance1",
      minCpu: 2,
      maxCpu: 8,
      minMemory: 16,
      maxMemory: 64,
      minOnDemandPrice: 0.01,
      maxOnDemandPrice: 0.1,
      minSpotPrice: 0.01,
      maxSpotPrice: 0.1,
      network: ["High", "Medium"],
      storage: ["EBS only", "10 GB HDD"],
      tagsAll: ["tag1", "tag2"],
      tagsAny: ["tag3", "tag4"],
      benchmark: "benchmark-series",
    };

    const query = service.transformFilterToQuery(filter);

    const expectedQuery = "SELECT i.name AS Name, i.on_demand_price AS \"On-Demand Price [$/h]\", i.spot_price AS \"Spot Price [$/h]\", " +
      "i.vcpu AS vCPUs, i.memory AS \"Memory [GB]\", i.network AS Network, i.storage AS Storage, " +
      "s.min AS Minimum, s.avg AS Average, s.median AS Median, s.max AS Maximum, i.tags AS Tags\n" +
      "FROM instances i, statistics s\n" +
      "WHERE i.name LIKE '%instance1%' " +
      "AND i.on_demand_price >= 0.01 AND i.on_demand_price <= 0.1 " +
      "AND i.spot_price >= 0.01 AND i.spot_price <= 0.1 " +
      "AND i.vcpu >= 2 AND i.vcpu <= 8 " +
      "AND i.memory >= 16 AND i.memory <= 64 " +
      "AND i.network IN ('High', 'Medium') " +
      "AND i.storage IN ('EBS only', '10 GB HDD') " +
      "AND i.id = s.instance_id AND s.benchmark_id = 'benchmark' AND s.series = 'series' " +
      "AND array_has_all(i.tags, ['tag1', 'tag2']) AND array_has_any(i.tags, ['tag3', 'tag4'])"
    expect(query).toBe(expectedQuery);
  });

  it("should handle an empty Filter object", () => {
    const filter: Filter = {};

    const query = service.transformFilterToQuery(filter);

    expect(query).toEqual(
      'SELECT i.name AS Name, i.on_demand_price AS "On-Demand Price [$/h]", i.spot_price AS "Spot Price [$/h]", ' +
      'i.vcpu AS vCPUs, i.memory AS "Memory [GB]", i.network AS Network, i.storage AS Storage, ' +
      'i.tags AS Tags\nFROM instances i\n'
    );
  });
});
