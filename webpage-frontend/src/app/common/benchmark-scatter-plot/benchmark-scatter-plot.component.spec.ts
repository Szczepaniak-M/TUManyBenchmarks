import {ComponentFixture, TestBed} from "@angular/core/testing";
import {BenchmarkScatterPlotComponent} from "./benchmark-scatter-plot.component";
import {Series} from "../benchmark-plot/benchmark-plot.model";
import {BenchmarkResult, Plot} from "../../instance-list/instance.model";
import {NgApexchartsModule} from "ng-apexcharts";
import {MockModule} from "ng-mocks";

describe("BenchmarkScatterPlotComponent", () => {
  let component: BenchmarkScatterPlotComponent;
  let fixture: ComponentFixture<BenchmarkScatterPlotComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [BenchmarkScatterPlotComponent],
      imports: [MockModule(NgApexchartsModule)]
    });

    fixture = TestBed.createComponent(BenchmarkScatterPlotComponent);
    component = fixture.componentInstance;
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should initialize chart options correctly", () => {
    const plot: Plot = {
      type: "scatter",
      title: "Sample Plot",
      yaxis: "Value",
      series: [
        {legend: "Series 1", y: "y1"},
        {legend: "Series 2", y: "y2"}
      ]
    };

    const benchmarkResults: BenchmarkResult[][] = [
      [{
        timestamp: 1,
        values: {
          y: 1
        }
      }]
    ];

    component.plot = plot;
    component.benchmarkResults = benchmarkResults;
    component.instances = [];
    fixture.detectChanges();

    expect(component.chartOptions.title!.text).toBe(plot.title);
    expect(component.chartOptions.xaxis!.title!.text).toBe("Execution time");
    expect(component.chartOptions.yaxis!.title!.text).toBe(plot.yaxis);
    expect(component.chartOptions.series!.length).toBeGreaterThan(0);
  });

  it("should generate correct series data for single instance", () => {
    const plot: Plot = {
      type: "scatter",
      title: "Test Plot",
      yaxis: "Y-Axis",
      series: [
        {legend: "Series 1", y: "y1"},
        {legend: "Series 2", y: "y2"}
      ]
    };

    const benchmarkResults: BenchmarkResult[][] = [
      [{
        timestamp: 1,
        values: {
          y1: 1,
          y2: 2
        }
      }]
    ];

    component.plot = plot;
    component.benchmarkResults = benchmarkResults;
    component.instances = [];
    fixture.detectChanges();

    const series = component.chartOptions.series as Series[];
    expect(series.length).toBe(2);
    for (let i = 0; i < 2; i++) {
      expect(series[i].type).toBe("scatter");
      expect(series[i].name).toContain(`Series ${i + 1}`);
    }
  });

  it("should generate correct series data for multiple instances", () => {
    const plot: Plot = {
      type: "scatter",
      title: "Test Plot",
      yaxis: "Y-Axis",
      series: [
        {legend: "Series 1", y: "y1"},
        {legend: "Series 2", y: "y2"}
      ]
    };

    const benchmarkResults: BenchmarkResult[][] = [
      [{
        timestamp: 1,
        values: {
          y1: 1,
          y2: 2
        }
      }],
      [{
        timestamp: 1,
        values: {
          y1: 1,
          y2: 2
        }
      }]
    ];

    const instances = ["Instance 1", "Instance 2"];

    component.plot = plot;
    component.benchmarkResults = benchmarkResults;
    component.instances = instances;
    fixture.detectChanges();

    const series = component.chartOptions.series as Series[];
    expect(series.length).toBe(4);
    for (let i = 0; i < 2; i++) {
      for (let j = 0; j < 2; j++) {
        expect(series[i * 2 + j].type).toBe("scatter");
        expect(series[i * 2 + j].name).toContain(`Instance ${i + 1} - Series ${j + 1}`);
      }
    }
  });
});
