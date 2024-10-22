import {ComponentFixture, TestBed} from "@angular/core/testing";
import {CompareInstancesBenchmarkComponent} from "./compare-instances-benchmark.component";
import {MockComponent} from "ng-mocks";
import {BenchmarkPlotComponent} from "../../common/benchmark-plot/benchmark-plot.component";
import {Instance} from "../../instance-list/instance.model";
import {environment} from "../../../environments/environment";

describe("CompareInstancesBenchmarkComponent", () => {
  let component: CompareInstancesBenchmarkComponent;
  let fixture: ComponentFixture<CompareInstancesBenchmarkComponent>;
  const mockInstances: Instance[] = [
    {
      name: "Instance 1",
      id: "",
      onDemandPrice: 0,
      spotPrice: 0,
      vcpu: 0,
      memory: 0,
      network: "",
      storage: "",
      tags: [],
      benchmarks: [
        {
          id: "benchmark1",
          name: "Benchmark 1",
          description: "Description 1",
          directory: "directory1",
          results: [{timestamp: 1, values: 1}, {timestamp: 1, values: 20}],
          plots: [{title: "Plot 1", type: "line", yaxis: "", series: []}]
        },
        {
          id: "benchmark2",
          name: "Benchmark 2",
          description: "Description 2",
          directory: "directory2",
          results: [{timestamp: 1, values: 1}, {timestamp: 1, values: 20}],
          plots: [{title: "Plot 1", type: "line", yaxis: "", series: []}]
        }
      ],
    },
    {
      name: "Instance 2",
      id: "",
      onDemandPrice: 0,
      spotPrice: 0,
      vcpu: 0,
      memory: 0,
      network: "",
      storage: "",
      tags: [],
      benchmarks: [{
        id: "benchmark1",
        name: "Benchmark 1",
        description: "Description 1",
        directory: "directory1",
        results: [{timestamp: 1, values: 10}, {timestamp: 1, values: 20}],
        plots: [{title: "Plot 1", type: "line", yaxis: "", series: []}],
      }],
    }
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [
        CompareInstancesBenchmarkComponent,
        MockComponent(BenchmarkPlotComponent)
      ]
    });

    fixture = TestBed.createComponent(CompareInstancesBenchmarkComponent);
    component = fixture.componentInstance;
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should initialize correctly", () => {
    component.benchmarkId = "benchmark1";
    component.instances = mockInstances;
    fixture.detectChanges();

    expect(component.benchmarkName).toBe("Benchmark 1");
    expect(component.benchmarkDescription).toBe("Description 1");
    expect(component.benchmarkDirectory).toBe("directory1");
    expect(component.benchmarkResults.length).toBe(2);
    expect(component.plots.length).toBe(1);
    expect(component.instanceNames).toEqual(["Instance 1", "Instance 2"]);
  });

  it("should render benchmark and its plot", () => {
    component.benchmarkId = "benchmark1";
    component.instances = mockInstances;
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector(".text-xl")?.textContent).toContain("Benchmark 1");
    expect(compiled.querySelector(".text-lg")?.getAttribute("href")).toContain(`${environment.repositoryUrl}/tree/main/directory1`);
    expect(compiled.querySelector(".text-base")?.textContent).toContain("Description 1");
    expect(compiled.querySelectorAll("app-benchmark-plot").length).toBe(1);
  });
});
