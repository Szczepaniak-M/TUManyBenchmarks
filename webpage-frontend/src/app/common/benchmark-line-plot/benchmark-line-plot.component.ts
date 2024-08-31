import {Component, Input, OnInit} from "@angular/core";
import {ChartOptions, DataPoint, Series} from "../benchmark-plot/benchmark-plot.model";
import {BenchmarkResult, Plot, PlotSeries} from "../../instance-list/instance.model";


@Component({
  selector: "app-benchmark-line-plot",
  template: `
    <apx-chart [series]="chartOptions.series!"
               [chart]="chartOptions.chart!"
               [colors]="chartOptions.colors!"
               [dataLabels]="chartOptions.dataLabels!"
               [title]="chartOptions.title!"
               [grid]="chartOptions.grid!"
               [xaxis]="chartOptions.xaxis!"
               [yaxis]="chartOptions.yaxis!"
               [fill]="chartOptions.fill!"
               [stroke]="chartOptions.stroke!"
               [legend]="chartOptions.legend!"
               [markers]="chartOptions.markers!"
               [tooltip]="chartOptions.tooltip!"
    ></apx-chart>
  `
})
export class BenchmarkLinePlotComponent implements OnInit {
  @Input({required: true}) benchmarkResults!: BenchmarkResult[][]
  @Input({required: true}) plot!: Plot;
  @Input() instances: string[] = [];
  chartOptions!: ChartOptions;

  ngOnInit(): void {
    const numberOfSeries = this.plot.series.length;
    const numberOfInstances = this.instances.length > 0 ? this.instances.length : 1
    this.chartOptions = {
      series: this.getBenchmarkSeries(this.benchmarkResults, this.plot.series, this.instances),
      chart: {
        animations: {
          enabled: false,
        },
        type: "rangeArea",
        height: "500px",
        zoom: {
          type: "xy",
        },
        toolbar: {
          export: {
            csv: {
              filename: this.plot.title.replaceAll(" ", "-"),
              headerCategory: this.plot.yaxis
            }
          }
        },
      },
      colors: this.repeatElements([
        "#D4526E",
        "#33B2DF",
        "#A133FF",
        "#FF9933",
        "#33FF57",
        "#FF33FF",
        "#FFFF44",
        "#44FFFF"
      ], 3),
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
        type: "numeric",
        decimalsInFloat: 5,
        title: {
          text: this.plot.xaxis,
          offsetX: -20,
          style: {
            fontSize: "14px"
          }
        },
      },
      yaxis: {
        decimalsInFloat: 5,
        logarithmic: !!this.plot.yaxisLog,
        logBase: this.plot.yaxisLog === 'e' ? Math.E : this.plot.yaxisLog,
        title: {
          text: this.plot.yaxis,
          style: {
            fontSize: "14px"
          }
        },
      },
      fill: {
        opacity: this.repeatArray([1, 0.25, 0.15], numberOfSeries * numberOfInstances)
      },
      stroke: {
        curve: "straight",
        width: this.repeatArray([2, 0, 0], numberOfSeries * numberOfInstances)
      },
      markers: {
        hover: {
          size: 5,
        }
      },
      tooltip: {
        x: {
          show: true,
          // original formater has problem with doubles when user hover the value
          formatter: function (value: any, opts: any): string {
            if (value === undefined || opts === undefined) {
              return `${value}`;
            }
            const newValue = opts.w.config.series[opts.seriesIndex].data[opts.dataPointIndex][0];
            return `${newValue}`;
          }
        }
      }
    };
  }

  private getBenchmarkSeries(benchmarkResults: BenchmarkResult[][], series: PlotSeries[], instances: string[]): Series[] {
    const result: Series[] = [];
    if (instances.length > 0) {
      this.extractSeriesForMultipleInstances(instances, series, benchmarkResults, result);
    } else {
      this.extractSeriesForSingleInstance(series, benchmarkResults, result);
    }
    return result;
  }

  private extractSeriesForSingleInstance(series: PlotSeries[], benchmarkResults: BenchmarkResult[][], result: Series[]) {
    for (const serie of series) {
      const yValues = this.getBenchmarkYValues(benchmarkResults[0], serie);
      const xValues = this.getBenchmarkXValues(serie, yValues, benchmarkResults[0]);

      const {averages, averagePlusStd, averageMinusStd, maxes, mins} = this.getStatistics(yValues);

      result.push({
        type: "line",
        name: `${serie.legend} - Average`,
        data: this.zipLists2(xValues, averages
        )
      });
      result.push({
        type: "rangeArea",
        name: `${serie.legend} - Std`,
        data: this.zipLists3(xValues, averageMinusStd, averagePlusStd)
      });
      result.push({
        type: "rangeArea",
        name: `${serie.legend} - Max/Min`,
        data: this.zipLists3(xValues, mins, maxes)
      });
    }
  }

  private extractSeriesForMultipleInstances(instances: string[], series: PlotSeries[], benchmarkResults: BenchmarkResult[][], result: Series[]) {
    for (const [index, instance] of instances.entries()) {
      for (const serie of series) {
        const yValues = this.getBenchmarkYValues(benchmarkResults[index], serie);
        const xValues = this.getBenchmarkXValues(serie, yValues, benchmarkResults[index]);

        const {averages, averagePlusStd, averageMinusStd, maxes, mins} = this.getStatistics(yValues);

        result.push({
          type: "line",
          name: `${instance} - ${serie.legend} - Average`,
          data: this.zipLists2(xValues, averages)
        });
        result.push({
          type: "rangeArea",
          name: `${instance} - ${serie.legend} - Std`,
          data: this.zipLists3(xValues, averageMinusStd, averagePlusStd)
        });
        result.push({
          type: "rangeArea",
          name: `${instance} - ${serie.legend} - Max/Min`,
          data: this.zipLists3(xValues, mins, maxes)
        });
      }
    }
  }

  private getStatistics(yValues: number[][]) {
    const averages = yValues.map(this.calculateAverage);
    const averagePlusStd: number[] = [];
    const averageMinusStd: number[] = [];
    yValues.forEach((list: number[], index: number) => {
      const stdDev = this.calculateStandardDeviation(list, averages[index]);
      averagePlusStd.push(stdDev + averages[index]);
      averageMinusStd.push(averages[index] - stdDev);
    });
    const maxes = yValues.map(this.calculateMax);
    const mins = yValues.map(this.calculateMin);
    return {averages, averagePlusStd, averageMinusStd, maxes, mins};
  }

  private getBenchmarkXValues(serie: PlotSeries, values: number[][], benchmarkResults: BenchmarkResult[]) {
    if (serie.x == "increasingValues") {
      return Array.from({length: values.length}, (_, i) => i + 1);
    } else {
      const extractedKeys: number[][] = benchmarkResults.map(benchmarkResult => benchmarkResult.values[serie.x!]);
      const transposedKeys = extractedKeys[0].map((_: any, colIndex: number) => extractedKeys.map(row => row[colIndex]));
      return transposedKeys.map(this.calculateAverage);
    }
  }

  private getBenchmarkYValues(benchmarkResults: BenchmarkResult[], serie: PlotSeries) {
    const values: number[][] = benchmarkResults.map(benchmarkResult => benchmarkResult.values[serie.y]);
    return values[0].map((_: any, colIndex: number) => values.map(row => row[colIndex]));
  }

  private calculateAverage(arr: number[]): number {
    const sum = arr.reduce((acc, val) => acc + val, 0);
    return Number((sum / arr.length).toPrecision(5));
  }

  private calculateStandardDeviation(arr: number[], avg: number): number {
    const squareDiffs = arr.map(val => Math.pow(val - avg, 2));
    const avgSquareDiff = this.calculateAverage(squareDiffs);
    return Number(Math.sqrt(avgSquareDiff).toPrecision(5));
  }

  private calculateMax(arr: number[]): number {
    return Number(Math.max(...arr).toPrecision(5));
  }

  private calculateMin(arr: number[]): number {
    return Number(Math.min(...arr).toPrecision(5));
  }

  private zipLists2(list1: number[], list2: number[]): DataPoint[] {
    return list1.map((element, index) => [element, list2[index]]);
  }

  private zipLists3(list1: number[], list2: number[], list3: number[]): DataPoint[] {
    return list1.map((element, index) => [element, [list2[index], list3[index]]]);
  }

  private repeatArray(arr: any[], repeat: number): any[] {
    let result: any[] = [];
    for (let i = 0; i < repeat; i++) {
      result = result.concat(arr);
    }
    return result;
  }

  private repeatElements(arr: any[], repeat: number): any[] {
    let result = [];
    for (let i = 0; i < arr.length; i++) {
      for (let j = 0; j < repeat; j++) {
        result.push(arr[i]);
      }
    }
    return result;
  }
}
