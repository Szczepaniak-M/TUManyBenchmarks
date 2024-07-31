export interface InstanceDetailsDto {
  id: string,
  name: string,
  tags: string[],
  benchmarks: Benchmark[]
}

export interface InstanceDetails {
  id: string,
  name: string,
  vcpu: number,
  memory: number,
  network: string,
  otherTags: string[],
  benchmarks: Benchmark[]
}

export interface Benchmark {
  id: string,
  name: string,
  description: string,
  results: BenchmarkResult[],
  plots: Plot[]
}

export interface BenchmarkResult {
  timestamp: number,
  values: any
}

export interface Plot {
  type: PlotType
  title: string,
  xaxis?: string,
  yaxis: string,
  series: PlotSeries[]
}

export interface PlotSeries {
  x?: string,
  y: string,
  legend: string
}

export type PlotType = "scatter" | "line";
