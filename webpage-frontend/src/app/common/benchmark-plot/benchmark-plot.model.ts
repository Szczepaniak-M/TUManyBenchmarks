import {
  ApexAxisChartSeries,
  ApexDataLabels, ApexFill,
  ApexGrid, ApexLegend,
  ApexMarkers,
  ApexStroke,
  ApexTitleSubtitle, ApexTooltip,
  ApexXAxis,
  ApexYAxis
} from "ng-apexcharts";

export type DataPoint = [number, any];

export interface Series {
  name: string,
  data: DataPoint[]
  type: string
}

export interface ChartOptions {
  series: ApexAxisChartSeries,
  colors: string[],
  chart: any, // TODO add type as ApexChart with version 1.12 when released
  xaxis: ApexXAxis,
  yaxis: ApexYAxis,
  dataLabels: ApexDataLabels,
  grid: ApexGrid,
  stroke: ApexStroke,
  title: ApexTitleSubtitle,
  markers: ApexMarkers,
  fill: ApexFill,
  legend: ApexLegend,
  tooltip: ApexTooltip
}
