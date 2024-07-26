export interface InstanceDetailsDto {
  id: string;
  name: string;
  tags: string[];
  benchmarks: Benchmark[];
}

export interface InstanceDetails {
  id: string;
  name: string;
  vcpu: number;
  memory: number;
  network: string;
  otherTags: string[];
  benchmarks: Benchmark[];
}

export interface Benchmark {
  id: string;
  name: string;
  description: string;
  results: BenchmarkResult[];
}

export interface BenchmarkResult {
  timestamp: number;
  values: any;
}
