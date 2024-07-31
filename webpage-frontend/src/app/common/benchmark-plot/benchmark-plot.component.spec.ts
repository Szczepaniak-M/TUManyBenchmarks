import {ComponentFixture, TestBed} from "@angular/core/testing";

import {BenchmarkPlotComponent} from "./benchmark-plot.component";
import {By} from "@angular/platform-browser";
import {BenchmarkScatterPlotComponent} from "../benchmark-scatter-plot/benchmark-scatter-plot.component";
import {BenchmarkLinePlotComponent} from "../benchmark-line-plot/benchmark-line-plot.component";
import {MockComponent} from "ng-mocks";

describe("BenchmarkPlotComponent", () => {
  let component: BenchmarkPlotComponent;
  let fixture: ComponentFixture<BenchmarkPlotComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [
        BenchmarkPlotComponent,
        MockComponent(BenchmarkScatterPlotComponent),
        MockComponent(BenchmarkLinePlotComponent)
      ]
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(BenchmarkPlotComponent);
    component = fixture.componentInstance;
  });

  it("should create the component", () => {
    expect(component).toBeTruthy();
  });

  it("should render BenchmarkScatterPlotComponent when plot type is scatter", () => {
    component.plot = {type: "scatter", title: "Scatter Plot", xaxis: "X", yaxis: "Y", series: []};
    component.benchmarkResults = [];
    fixture.detectChanges();

    const scatterPlotElement = fixture.debugElement.query(By.css("app-benchmark-scatter-plot"));
    const linePlotElement = fixture.debugElement.query(By.css("app-benchmark-line-plot"));

    expect(scatterPlotElement).toBeTruthy();
    expect(linePlotElement).toBeNull();
  });

  it("should render BenchmarkLinePlotComponent when plot type is line", () => {
    component.plot = {type: "line", title: "Line Plot", xaxis: "X", yaxis: "Y", series: []};
    component.benchmarkResults = [];
    fixture.detectChanges();

    const linePlotElement = fixture.debugElement.query(By.css("app-benchmark-line-plot"));
    const scatterPlotElement = fixture.debugElement.query(By.css("app-benchmark-scatter-plot"));

    expect(linePlotElement).toBeTruthy();
    expect(scatterPlotElement).toBeNull();
  });
});
