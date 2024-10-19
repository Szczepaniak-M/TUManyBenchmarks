import {ComponentFixture, TestBed} from "@angular/core/testing";
import {InstanceListComponent} from "./instance-list.component";
import {InstanceListService} from "./instance-list.service";
import {Router} from "@angular/router";
import {of} from "rxjs";
import {BenchmarkDetails, BenchmarkStatistics, Instance, InstanceDefaultRow} from "./instance.model";
import {SortEvent} from "./list-header/list-sort/list-sort.model";
import {By} from "@angular/platform-browser";
import {ListSortComponent} from "./list-header/list-sort/list-sort.component";
import {MockComponent} from "ng-mocks";
import {ListFilterComponent} from "./list-filter/list-filter.component";
import {ListRowComponent} from "./list-row/list-row.component";
import {ListQueryComponent} from "./list-query/list-query.component";
import {ListQueryService} from "./list-query/list-query.service";
import {ListHeaderComponent} from "./list-header/list-header.component";

describe("InstanceListComponent", () => {
  let component: InstanceListComponent;
  let fixture: ComponentFixture<InstanceListComponent>;
  let mockInstanceListService: { getInstances: any, getBenchmarks: any, getStatistics: any };
  let mockListQueryService: { initializeDuckDB: any, loadDatabase: any };
  let mockRouter: { navigate: any; };
  let mockInstances: Instance[];
  let mockDefaultRows: InstanceDefaultRow[];
  let mockBenchmarks: BenchmarkDetails[];
  let mockStatistics: BenchmarkStatistics[];

  beforeEach(() => {
    mockInstances = [
      {
        id: "id1", name: "t2.micro", vcpu: 4, memory: 16, onDemandPrice: 0.01, spotPrice: 0.001, network: "Network1", tags: ["tag1", "tag2"], benchmarks: [
          {
            id: "benchmark1", name: "Benchmark 1", description: "Description 1",
            directory: "directory1", results: [], plots: []
          },
          {
            id: "benchmark2", name: "Benchmark 2", description: "Description 2",
            directory: "directory2", results: [], plots: []
          }
        ]
      },
      {
        id: "id2", name: "t2.nano", vcpu: 2, memory: 8, onDemandPrice: 0.005, spotPrice: 0.0005, network: "Network1", tags: ["tag1", "tag3"], benchmarks: [
          {
            id: "benchmark1", name: "Benchmark 1", description: "Description 1",
            directory: "directory1", results: [], plots: []
          }
        ]
      },
      {
        id: "id3", name: "t2.small", vcpu: 8, memory: 32, onDemandPrice: 0.02, spotPrice: 0.002, network: "Network2", tags: ["tag1"], benchmarks: [
          {
            id: "benchmark2", name: "Benchmark 2", description: "Description 2",
            directory: "directory2", results: [], plots: []
          }
        ]
      },
    ];

    mockBenchmarks = [
      {
        id: "benchmark1", name: "Benchmark 1", description: "Description 1",
        instanceTypes: ["t2.micro", "t2.nano"], instanceTags: [],
        seriesX: [], seriesY: ["Series1"], seriesOther: []
      },
      {
        id: "benchmark2", name: "Benchmark 2", description: "Description 2",
        instanceTypes: ["t2.micro", "t2.small"], instanceTags: [],
        seriesX: ["increasing_values"], seriesY: ["Series2"], seriesOther: []
      },
    ]

    mockStatistics = [
      {
        instanceId: "id1", benchmarkId: "benchmark1", series: "Series1",
        min: 0, max: 0, avg: 0, median: 0
      },
      {
        instanceId: "id1", benchmarkId: "benchmark2", series: "Series2",
        min: 0, max: 0, avg: 0, median: 0
      },
      {
        instanceId: "id2", benchmarkId: "benchmark1", series: "Series1",
        min: 0, max: 0, avg: 0, median: 0
      },
      {
        instanceId: "id3", benchmarkId: "benchmark2", series: "Series2",
        min: 0, max: 0, avg: 0, median: 0
      }
    ]

    mockDefaultRows = [
      {
        id: 0, Name: "t2.micro", "On-Demand Price [$/h]": 0.01, "Spot Price [$/h]": 0.001, vCPUs: 4, Memory: 16, Network: "Network1", Tags: ["tag1", "tag2"],
        benchmarks: [mockStatistics[0], mockStatistics[1]], hidden: false
      },
      {
        id: 1, Name: "t2.nano", "On-Demand Price [$/h]": 0.005, "Spot Price [$/h]": 0.0005,vCPUs: 2, Memory: 8, Network: "Network1", Tags: ["tag1", "tag3"],
        benchmarks: [mockStatistics[2]], hidden: false
      },
      {
        id: 2, Name: "t2.small", "On-Demand Price [$/h]": 0.02, "Spot Price [$/h]": 0.002,vCPUs: 8, Memory: 32, Network: "Network2", Tags: ["tag1"],
        benchmarks: [mockStatistics[3]], hidden: false
      },
    ];

    mockInstanceListService = {
      getInstances: jasmine.createSpy("getInstances").and.returnValue(of(mockInstances)),
      getBenchmarks: jasmine.createSpy("getBenchmarks").and.returnValue(of(mockBenchmarks)),
      getStatistics: jasmine.createSpy("getStatistics").and.returnValue(of(mockStatistics)),
    };
    mockListQueryService = {
      initializeDuckDB: jasmine.createSpy("initializeDuckDB").and.returnValue(Promise.resolve()),
      loadDatabase: jasmine.createSpy("loadDatabase"),
    }
    mockRouter = {
      navigate: jasmine.createSpy("navigate").and.returnValue(Promise.resolve(true))
    };

    TestBed.configureTestingModule({
      declarations: [
        InstanceListComponent,
        MockComponent(ListFilterComponent),
        MockComponent(ListQueryComponent),
        MockComponent(ListHeaderComponent),
        MockComponent(ListRowComponent)
      ],
      providers: [
        {provide: InstanceListService, useValue: mockInstanceListService},
        {provide: ListQueryService, useValue: mockListQueryService},
        {provide: Router, useValue: mockRouter}
      ]
    });

    fixture = TestBed.createComponent(InstanceListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should initialize on ngOnInit", () => {
    expect(mockInstanceListService.getInstances).toHaveBeenCalled();
    expect(mockInstanceListService.getBenchmarks).toHaveBeenCalled();
    expect(mockInstanceListService.getStatistics).toHaveBeenCalled();

    expect(component.defaultRows.length).toBe(3);
    expect(component.rows.length).toBe(3);
    expect(component.rows).toEqual(mockDefaultRows);
    expect(component.allTags).toEqual(["tag1", "tag2", "tag3"]);
    expect(component.allNetworks).toEqual(["Network1", "Network2"]);
    expect(component.allBenchmarks).toEqual([
      {name: "Benchmark 1 - Series1", id: "benchmark1-Series1"},
      {name: "Benchmark 2 - Series2", id: "benchmark2-Series2"}
    ]);
    expect(component.queryConsoleActive).toBeFalse();
    expect(component.columns).toEqual(["Name", "On-Demand Price [$/h]", "Spot Price [$/h]", "vCPUs", "Memory", "Network", "Tags"]);
  });

  it("should filter instances based on filter criteria", () => {
    const filter = {
      name: "t2",
      minOnDemandPrice: 0.01,
      maxOnDemandPrice: 0.02,
      minSpotPrice: 0.001,
      maxSpotPrice: 0.002,
      minCpu: 4,
      maxCpu: 64,
      minMemory: 8,
      maxMemory: 32,
      network: ["Network1"],
      tagsAll: ["tag1"],
      tagsAny: ["tag1"],
      benchmark: "benchmark1-Series1"
    };
    component.applyFilters(filter);
    fixture.detectChanges();
    expect(component.rows.length).toBe(3);
    expect(component.rows[0]["hidden"]).toBeFalse();
    expect(component.rows[1]["hidden"]).toBeTrue();
    expect(component.rows[2]["hidden"]).toBeTrue();
    expect(component.countRows()).toEqual(1)
  });

  it("should add and remove instances from comparison", () => {
    expect(component.selectedInstances.size).toEqual(0);
    component.toggleComparison(mockInstances[0]);
    expect(component.selectedInstances.size).toBe(1);
    expect(component.selectedInstances.has("t2.micro")).toBeTrue();
    component.toggleComparison(mockInstances[0]);
    expect(component.selectedInstances.size).toBe(0);
    expect(component.selectedInstances.has("t2.micro")).toBeFalse();
  });

  it("should not allow for adding more than 3 instances", () => {
    expect(component.selectedInstances.size).toEqual(0);
    component.toggleComparison(mockInstances[0]);
    component.toggleComparison(mockInstances[1]);
    component.toggleComparison(mockInstances[2]);
    expect(component.selectedInstances.size).toBe(3);
    component.toggleComparison({name: "otherInstance"});
    expect(component.selectedInstances.size).toBe(3);
    expect(component.selectedInstances.has("otherInstance")).toBeFalse();
    component.toggleComparison(mockInstances[0]);
    component.toggleComparison({name: "otherInstance"});
    expect(component.selectedInstances.size).toBe(3);
    expect(component.selectedInstances.has("otherInstance")).toBeTrue();
  });

  it("should navigate to comparison page with selected instances", () => {
    component.selectedInstances.add(mockInstances[0].name);
    component.selectedInstances.add(mockInstances[1].name);
    component.compareSelectedItems();
    expect(mockRouter.navigate).toHaveBeenCalledWith(["/instance/compare"], {
      queryParams: {instances: "t2.micro,t2.nano"}
    });
  });

  it("should correctly identify if an instance is in comparison", () => {
    component.selectedInstances.add(mockInstances[0].name);
    expect(component.isInComparison(mockInstances[0])).toBeTrue();
    expect(component.isInComparison(mockInstances[1])).toBeFalse();
  });

  it("should sort instances based on the selected column and direction", () => {
    const sortEvent: SortEvent = {column: "Memory", direction: "desc"};
    component.onSort(sortEvent);
    fixture.detectChanges()
    expect(component.rows[0]["Memory"]).toBeGreaterThanOrEqual(component.rows[1]["Memory"]);
  });

  it("should track instances by id", () => {
    const instance = component.rows[0];
    expect(component.trackById(0, instance)).toBe(0);
  });

  it("should reset sorting direction for other columns when sorting by a new column", () => {
    const sortComponents = fixture.debugElement.queryAll(By.directive(ListSortComponent));
    sortComponents.forEach(sortComponent => {
      spyOn(sortComponent.componentInstance, "resetDirection");
    });

    component.onSort({column: "memory", direction: "asc"});
    sortComponents.forEach(sortComponent => {
      if (sortComponent.componentInstance.column !== "memory") {
        expect(sortComponent.componentInstance.resetDirection).toHaveBeenCalled();
      }
    });
  });

  it("should show and hide the query console", () => {
    component.onShowConsoleClick();
    fixture.detectChanges();
    expect(component.queryConsoleActive).toBeTrue();

    component.onShowConsoleClick();
    fixture.detectChanges();
    expect(component.queryConsoleActive).toBeFalse();
  });

  it("should reset rows when query console is hidden", () => {
    component.queryConsoleActive = true;
    component.rows = [];
    component.onShowConsoleClick();
    fixture.detectChanges();
    expect(component.rows.length).toBe(3);
  });
});

