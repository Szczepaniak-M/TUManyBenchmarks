import {BenchmarkResult, Plot} from "../../instance-details/instance-details.model";

export interface CompareInstancesBenchmark {
  instanceName: string,
  benchmarkResults: BenchmarkResult[],
  plots: Plot[]
}
