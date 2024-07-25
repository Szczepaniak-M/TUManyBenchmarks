import {
  ApexAxisChartSeries,
  ApexChart,
  ApexDataLabels,
  ApexGrid, ApexLegend, ApexMarkers,
  ApexStroke, ApexTitleSubtitle,
  ApexXAxis,
  ApexYAxis
} from "ng-apexcharts";

export interface Series {
  name: string,
  data: number[]
}

export interface ChartOptions {
  series: ApexAxisChartSeries,
  colors: string[],
  chart: ApexChart,
  xaxis: ApexXAxis,
  yaxis: ApexYAxis,
  dataLabels: ApexDataLabels,
  grid: ApexGrid,
  stroke: ApexStroke,
  title: ApexTitleSubtitle,
  legend: ApexLegend,
  markers: ApexMarkers,
}
