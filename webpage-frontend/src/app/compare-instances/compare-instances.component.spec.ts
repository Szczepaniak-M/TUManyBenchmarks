import {ComponentFixture, TestBed} from "@angular/core/testing";
import {CompareInstancesComponent} from "./compare-instances.component";
import {ActivatedRoute} from "@angular/router";
import {InstanceDetailsService} from "../instance-details/instance-details.service";
import {of} from "rxjs";
import {By} from "@angular/platform-browser";
import {MatSlideToggle} from "@angular/material/slide-toggle";
import {MockComponent} from "ng-mocks";
import {CompareInstancesBenchmarkComponent} from "./compare-instances-benchmark/compare-instances-benchmark.component";

describe("CompareInstancesComponent", () => {
  let component: CompareInstancesComponent;
  let fixture: ComponentFixture<CompareInstancesComponent>;
  let mockInstanceDetailsService: { getInstanceDetails: any };

  beforeEach(() => {
    mockInstanceDetailsService = {
      getInstanceDetails: jasmine.createSpy("getInstanceDetails").and.callFake(
        (name: string) => of({
          id: "id",
          name: name,
          onDemandPrice: 0.1,
          spotPrice: 0.01,
          vcpu: 4,
          memory: 16,
          network: "10 Gbps",
          storage: "EBS only",
          tags: ["Tag1", "Tag2"],
          benchmarks: [
            {id: "benchmark1", name: "Benchmark 1", description: "Description 1", results: [], plots: []},
            {id: "benchmark2", name: "Benchmark 2", description: "Description 2", results: [], plots: []}
          ]
        }))
    };

    TestBed.configureTestingModule({
      imports: [
        MockComponent(MatSlideToggle)
      ],
      declarations: [
        CompareInstancesComponent,
        MockComponent(CompareInstancesBenchmarkComponent)
      ],
      providers: [
        {provide: ActivatedRoute, useValue: {snapshot: {queryParamMap: {get: () => "t2.micro,t2.small"}}}},
        {provide: InstanceDetailsService, useValue: mockInstanceDetailsService}
      ]
    });

    fixture = TestBed.createComponent(CompareInstancesComponent);
    component = fixture.componentInstance;
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should initialize and fetch instance details", () => {
    fixture.detectChanges();
    expect(mockInstanceDetailsService.getInstanceDetails).toHaveBeenCalledWith("t2.micro");
    expect(mockInstanceDetailsService.getInstanceDetails).toHaveBeenCalledWith("t2.small");
    expect(component.instances.length).toBe(2);
    expect(component.instances[0].name).toBe("t2.micro");
    expect(component.instances[1].name).toBe("t2.small");
  });

  it("should toggle showOnlyCommon property", () => {
    component.toggleShowOnlyCommon();
    expect(component.showOnlyCommon).toBeTrue();

    component.toggleShowOnlyCommon();
    expect(component.showOnlyCommon).toBeFalse();
  });

  it("should calculate benchmarkIds and commonBenchmarkIds", () => {
    fixture.detectChanges();
    expect(component.benchmarkIds.size).toBe(2);
    expect(component.commonBenchmarkIds.size).toBe(2);
  });

  it("should render instance details", () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const instanceNames = compiled.querySelectorAll("h1.text-2xl");
    expect(instanceNames.length).toBe(3);
    expect(instanceNames[1].textContent).toContain("t2.micro");
    expect(instanceNames[2].textContent).toContain("t2.small");
  });

  it("should render benchmark components", () => {
    fixture.detectChanges();
    const benchmarkComponents = fixture.debugElement.queryAll(By.css("app-compare-instances-benchmark"));
    expect(benchmarkComponents.length).toBe(2);
  });
});
