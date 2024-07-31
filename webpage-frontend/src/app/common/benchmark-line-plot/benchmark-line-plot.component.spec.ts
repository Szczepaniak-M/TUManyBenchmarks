import {ComponentFixture, TestBed} from "@angular/core/testing";
import {BenchmarkLinePlotComponent} from "./benchmark-line-plot.component";
import {Series} from "../benchmark-plot/benchmark-plot.model";
import {BenchmarkResult, Plot} from "../../instance-details/instance-details.model";
import {NgApexchartsModule} from "ng-apexcharts";
import {MockModule} from "ng-mocks";

describe("BenchmarkLinePlotComponent", () => {
  let component: BenchmarkLinePlotComponent;
  let fixture: ComponentFixture<BenchmarkLinePlotComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [BenchmarkLinePlotComponent],
      imports: [MockModule(NgApexchartsModule)]
    });

    fixture = TestBed.createComponent(BenchmarkLinePlotComponent);
    component = fixture.componentInstance;
  });

  it("should create the component", () => {
    expect(component).toBeTruthy();
  });

  it("should initialize chart options correctly", () => {
    const plot: Plot = {
      type: "line",
      title: "Test Plot",
      xaxis: "X-Axis",
      yaxis: "Y-Axis",
      series: [
        {legend: "Series 1", x: "x", y: "y"},
        {legend: "Series 2", x: "x", y: "y"}
      ]
    };

    const benchmarkResults: BenchmarkResult[][] = [
      [{
        timestamp: 1,
        values: {
          x: [1, 2, 3],
          y: [4, 5, 6]
        }
      }]
    ];

    component.plot = plot;
    component.benchmarkResults = benchmarkResults;
    component.instances = [];
    fixture.detectChanges();

    expect(component.chartOptions.title!.text).toBe(plot.title);
    expect(component.chartOptions.xaxis!.title!.text).toBe(plot.xaxis);
    expect(component.chartOptions.yaxis!.title!.text).toBe(plot.yaxis);
    expect(component.chartOptions.series!.length).toBeGreaterThan(0);
  });

  it("should generate correct series data for single instance", () => {
    const plot: Plot = {
      type: "line",
      title: "Test Plot",
      xaxis: "X-Axis",
      yaxis: "Y-Axis",
      series: [
        {legend: "Series 1", x: "x1", y: "y1"},
        {legend: "Series 2", x: "x2", y: "y2"}
      ]
    };

    const benchmarkResults: BenchmarkResult[][] = [
      [{
        timestamp: 1,
        values: {
          x1: [1, 2, 3],
          y1: [4, 5, 6],
          x2: [1, 2, 3],
          y2: [4, 5, 6]
        }
      }]
    ];

    component.plot = plot;
    component.benchmarkResults = benchmarkResults;
    component.instances = [];
    fixture.detectChanges();

    const series = component.chartOptions.series as Series[];
    expect(series.length).toBe(6);
    for (let i = 0; i < 2; i++) {
      expect(series[i * 3].type).toBe("line");
      expect(series[i * 3].name).toContain(`Series ${i + 1} - Average`);
      expect(series[i * 3 + 1].type).toBe("rangeArea");
      expect(series[i * 3 + 1].name).toContain(`Series ${i + 1} - Std`);
      expect(series[i * 3 + 2].type).toBe("rangeArea");
      expect(series[i * 3 + 2].name).toContain(`Series ${i + 1} - Max/Min`);
    }
  });

  it("should generate correct series data for multiple instances", () => {
    const plot: Plot = {
      type: "line",
      title: "Test Plot",
      xaxis: "X-Axis",
      yaxis: "Y-Axis",
      series: [
        {legend: "Series 1", x: "x1", y: "y1"},
        {legend: "Series 2", x: "x2", y: "y2"}
      ]
    };

    const benchmarkResults: BenchmarkResult[][] = [
      [{
        timestamp: 1,
        values: {
          x1: [1, 2, 3],
          y1: [4, 5, 6],
          x2: [1, 2, 3],
          y2: [4, 5, 6]
        }
      }],
      [{
        timestamp: 1,
        values: {
          x1: [1, 2, 3],
          y1: [4, 5, 6],
          x2: [1, 2, 3],
          y2: [4, 5, 6]
        }
      }]
    ];

    const instances = ["Instance 1", "Instance 2"];

    component.plot = plot;
    component.benchmarkResults = benchmarkResults;
    component.instances = instances;
    fixture.detectChanges();

    const series = component.chartOptions.series as Series[];
    expect(series.length).toBe(12);
    for (let i = 0; i < 2; i++) {
      for (let j = 0; j < 2; j++) {
        expect(series[i * 6 + j * 3].type).toBe("line");
        expect(series[i * 6 + j * 3].name).toContain(`Instance ${i + 1} - Series ${j + 1} - Average`);
        expect(series[i * 6 + j * 3 + 1].type).toBe("rangeArea");
        expect(series[i * 6 + j * 3 + 1].name).toContain(`Instance ${i + 1} - Series ${j + 1} - Std`);
        expect(series[i * 6 + j * 3 + 2].type).toBe("rangeArea");
        expect(series[i * 6 + j * 3 + 2].name).toContain(`Instance ${i + 1} - Series ${j + 1} - Max/Min`);
      }
    }
  });
});
