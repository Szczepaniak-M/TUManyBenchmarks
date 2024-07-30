import {Component, Input, OnInit} from "@angular/core";
import {ChartOptions, DataPoint, Series} from "../benchmark-plot/benchmark-plot.model";
import {BenchmarkResult, Plot, PlotSeries} from "../../instance-details/instance-details.model";


@Component({
  selector: "app-benchmark-scatter-plot",
  template: `
    <div style="text-align:center">
      <apx-chart [series]="chartOptions.series!"
                 [chart]="chartOptions.chart!"
                 [colors]="chartOptions.colors!"
                 [dataLabels]="chartOptions.dataLabels!"
                 [title]="chartOptions.title!"
                 [grid]="chartOptions.grid!"
                 [xaxis]="chartOptions.xaxis!"
                 [yaxis]="chartOptions.yaxis!"
                 [markers]="chartOptions.markers!"
                 [tooltip]="chartOptions.tooltip!"
      ></apx-chart>
    </div>
  `
})
export class BenchmarkScatterPlotComponent implements OnInit {
  @Input({required: true}) benchmarkResults!: BenchmarkResult[][]
  @Input({required: true}) plot!: Plot;
  @Input() instances: string[] = [];
  chartOptions!: Partial<ChartOptions>;

  ngOnInit(): void {
    this.chartOptions = {
      series: this.getBenchmarkSeries(this.benchmarkResults, this.plot.series, this.instances),
      chart: {
        height: "500px",
        type: "scatter",
        zoom: {
          type: "xy",
        },
        toolbar: {
          export: {
            csv: {
              filename: this.plot.title.replaceAll(" ", "-"),
              headerCategory: "timestamp",
              categoryFormatter: (cat: any) => `${cat / 1000}`,
            }
          }
        },
      },
      colors: [
        "#3357FF",
        "#FF5733",
        "#33FF57",
        "#FF33A1",
        "#33FFF6",
        "#1E90FF",
        "#FFFF33",
        "#A133FF",
        "#FF9933",
        "#FF3388",
      ],
      dataLabels: {
        enabled: false
      },
      title: {
        text: this.plot.title,
        align: "center",
        style: {
          fontSize: "20px"
        }
      },
      grid: {
        xaxis: {
          lines: {
            show: true,
          },
        },
        yaxis: {
          lines: {
            show: true,
          },
        },
      },
      xaxis: {
        type: "datetime",
        offsetX: -33,
        decimalsInFloat: 5,
        title: {
          text: "Execution time",
          style: {
            fontSize: "14px"
          }
        },
      },
      yaxis: {
        decimalsInFloat: 5,
        title: {
          text: this.plot.yaxis,
          style: {
            fontSize: "14px"
          }
        },
      },
      markers: {
        shape: "circle",
        size: 3,
        fillOpacity: 0.8,
        strokeColors: "#333",
        strokeWidth: 1,
      },
      tooltip: {
        x: {
          show: true,
          format: "yyyy-MM-dd HH:mm"
        }
      }
    };
  }

  private getBenchmarkSeries(benchmarkResults: BenchmarkResult[][], plotSeries: PlotSeries[], instances: string[]): Series[] {
    const seriesMap = new Map<string, DataPoint[]>()
    const result: Series[] = [];
    if (instances.length > 0) {
      this.extractSeriesForMultipleInstances(instances, plotSeries, seriesMap, benchmarkResults);
    } else {
      this.extractSeriesForSingleInstance(plotSeries, seriesMap, benchmarkResults);
    }
    seriesMap.forEach((value, key) => {
      result.push({type: "scatter", name: key, data: value});
    })
    return result
  }

  private extractSeriesForSingleInstance(plotSeries: PlotSeries[], seriesMap: Map<string, DataPoint[]>, benchmarkResults: BenchmarkResult[][]) {
    for (const series of plotSeries) {
      seriesMap.set(series.legend, [])
    }
    for (const benchmarkResult of benchmarkResults[0]) {
      for (const series of plotSeries) {
        seriesMap.get(series.legend)!!.push([benchmarkResult.timestamp * 1000, benchmarkResult.values[series.y]])
      }
    }
  }

  private extractSeriesForMultipleInstances(instances: string[], plotSeries: PlotSeries[], seriesMap: Map<string, DataPoint[]>, benchmarkResults: BenchmarkResult[][]) {
    for (const [index, instance] of instances.entries()) {
      for (const series of plotSeries) {
        seriesMap.set(`${instance} - ${series.legend}`, [])
      }
      for (const benchmarkResult of benchmarkResults[index]) {
        for (const series of plotSeries) {
          seriesMap.get(`${instance} - ${series.legend}`)!!.push([benchmarkResult.timestamp * 1000, benchmarkResult.values[series.y]])
        }
      }
    }
  }
}
