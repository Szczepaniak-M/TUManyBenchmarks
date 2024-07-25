import {Component, Input, ViewChild} from "@angular/core";
import {ChartComponent} from "ng-apexcharts";
import {ChartOptions, Series} from "./series.model";


@Component({
  selector: "app-benchmark-plot",
  template: `
    <div style="text-align:center">
      <apx-chart
        [series]="chartOptions.series!"
        [chart]="chartOptions.chart!"
        [xaxis]="chartOptions.xaxis!"
        [title]="chartOptions.title!"
      ></apx-chart>
    </div>
  `
})
export class ScatterBenchmarkPlotComponent {
  @Input() series: Series[] = []
  @Input() title: string = ""
  @ViewChild("chart") chart!: ChartComponent;
  chartOptions: Partial<ChartOptions>;

  constructor() {

    this.chartOptions = {
      series: this.series,
      chart: {
        height: 350,
        type: "scatter",
        zoom: {
          type: 'xy',
        }
      },
      colors: [
        '#3357FF',
        '#FF5733',
        '#33FF57',
        '#FF33A1',
        '#33FFF6',
        '#1E90FF',
        '#FFFF33',
        '#A133FF',
        '#FF9933',
        '#FF3388',
      ],
      dataLabels: {
        enabled: false
      },
      title: {
        text: this.title,
        align: "left"
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
        type: 'datetime',
      },
      yaxis: {},
      legend: {},
      markers: {
        shape: "circle",
        size: 10,
        fillOpacity: 0.8,
        strokeColors: '#333',
        strokeWidth: 1,
      },
    };
  }

}
