export interface Filter {
  name?: string,
  minCpu?: number,
  maxCpu?: number,
  minMemory?: number
  maxMemory?: number,
  network?: string[],
  tags?: string[]
}
