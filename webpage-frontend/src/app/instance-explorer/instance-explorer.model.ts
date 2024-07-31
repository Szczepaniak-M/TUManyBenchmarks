export interface InstanceExplorerRequest {
  partialResults: boolean,
  aggregationStages: string[]
}

export interface InstanceExplorerResponse {
  totalQueries: number,
  successfulQuires: number,
  results: string[],
  error?: string
}
