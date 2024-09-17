import {ComponentFixture, TestBed} from "@angular/core/testing";
import {InstanceDetailsComponent} from "./instance-details.component";
import {ActivatedRoute} from "@angular/router";
import {InstanceDetailsService} from "./instance-details.service";
import {of} from "rxjs";
import {By} from "@angular/platform-browser";
import {MockComponent} from "ng-mocks";
import {BenchmarkPlotComponent} from "../common/benchmark-plot/benchmark-plot.component";

describe("InstanceDetailsComponent", () => {
  let component: InstanceDetailsComponent;
  let fixture: ComponentFixture<InstanceDetailsComponent>;
  let mockInstanceDetailsService: { getInstanceDetails: any };

  beforeEach(() => {
    mockInstanceDetailsService = {
      getInstanceDetails: jasmine.createSpy("getInstanceDetails").and.callFake(
        (name: string) => of({
          id: "id",
          name: name,
          onDemandPrice: 0.01,
          spotPrice: 0.001,
          vcpu: 4,
          network: "10 Gbps",
          memory: 16,
          tags: ["Tag1", "Tag2"],
          benchmarks: [
            {
              id: "benchmark1", name: "Benchmark 1", description: "Description 1", results: [],
              plots: [{type: "scatter", title: "X", yaxis: "Y", series: []}]
            },
            {
              id: "benchmark2", name: "Benchmark 2", description: "Description 2", results: [],
              plots: [{type: "scatter", title: "X", yaxis: "Y", series: []}]
            }
          ]
        }))
    };

    TestBed.configureTestingModule({
      declarations: [
        InstanceDetailsComponent,
        MockComponent(BenchmarkPlotComponent)
      ],
      providers: [
        {provide: ActivatedRoute, useValue: {snapshot: {paramMap: {get: () => "t2.micro"}}}},
        {provide: InstanceDetailsService, useValue: mockInstanceDetailsService}
      ]
    })

    fixture = TestBed.createComponent(InstanceDetailsComponent);
    component = fixture.componentInstance;
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should fetch instance details on init", () => {
    fixture.detectChanges();
    expect(mockInstanceDetailsService.getInstanceDetails).toHaveBeenCalledWith("t2.micro");
    expect(component.instance.name).toBe("t2.micro");
  });

  it("should display instance details correctly", () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector(".text-3xl")?.textContent).toContain("t2.micro");
    expect(compiled.querySelectorAll("p")[0].textContent).toContain("On-Demand Price: $0.01 hourly");
    expect(compiled.querySelectorAll("p")[1].textContent).toContain("Spot Price: $0.001 hourly");
    expect(compiled.querySelectorAll("p")[2].textContent).toContain("vCPU: 4");
    expect(compiled.querySelectorAll("p")[3].textContent).toContain("Network: 10 Gbps");
    expect(compiled.querySelectorAll("p")[4].textContent).toContain("Memory: 16 GiB");
  });

  it("should display benchmarks correctly", () => {
    fixture.detectChanges();

    const benchmarkTitles = fixture.debugElement.queryAll(By.css(".text-lg"));
    expect(benchmarkTitles.length).toBe(2);
    expect(benchmarkTitles[0].nativeElement.textContent).toContain("Benchmark 1");
    expect(benchmarkTitles[1].nativeElement.textContent).toContain("Benchmark 2");

    const plotComponents = fixture.debugElement.queryAll(By.css("app-benchmark-plot"));
    expect(plotComponents.length).toBe(2);
  });

  it("should display a message if no instance details found", () => {
    fixture.detectChanges();
    component.instance = null!;
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain("Instance details not found.");
  });
});
