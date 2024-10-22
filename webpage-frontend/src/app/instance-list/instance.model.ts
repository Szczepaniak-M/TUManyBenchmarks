export interface InstanceDefaultRow {
  id: number;
  Name: string,
  "On-Demand Price [$/h]": number,
  "Spot Price [$/h]": number,
  vCPUs: number,
  "Memory [GB]": number,
  Network: string,
  Storage: string,
  Tags: string[],
  Minimum?: number,
  Average?: number,
  Median?: number,
  Maximum?: number,
  benchmarks: BenchmarkStatistics[],
  hidden: boolean,
}

export interface Instance {
  id: string,
  name: string,
  onDemandPrice: number,
  spotPrice: number,
  vcpu: number,
  memory: number,
  network: string,
  storage: string,
  tags: string[],
  benchmarks: Benchmark[],
}

export interface Benchmark {
  id: string,
  name: string,
  description: string,
  directory: string,
  results: BenchmarkResult[],
  plots: Plot[],
}

export interface BenchmarkResult {
  timestamp: number,
  values: any,
}

export interface Plot {
  type: PlotType,
  title: string,
  xaxis?: string,
  yaxis: string,
  yaxisLog?: number | 'e',
  series: PlotSeries[],
}

export interface PlotSeries {
  x?: string,
  y: string,
  legend: string,
}

export type PlotType = "scatter" | "line";

export interface BenchmarkDetails {
  id: string,
  name: string,
  description: string,
  instanceTypes: string[],
  instanceTags: string[][],
  seriesX: string[],
  seriesY: string[],
  seriesOther: string[],
}

export interface BenchmarkStatistics{
  instanceId: string,
  benchmarkId: string,
  series: string,
  min: number,
  max: number,
  avg: number,
  median: number,
}
