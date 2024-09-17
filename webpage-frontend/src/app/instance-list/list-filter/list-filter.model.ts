export interface Filter {
  name?: string,
  minOnDemandPrice?: number,
  maxOnDemandPrice?: number,
  minSpotPrice?: number,
  maxSpotPrice?: number,
  minCpu?: number,
  maxCpu?: number,
  minMemory?: number,
  maxMemory?: number,
  network?: string[],
  tags?: string[],
  benchmark?: string,
}
