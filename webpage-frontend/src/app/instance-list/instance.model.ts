export interface InstanceDto {
  id: string,
  name: string,
  tags: string[]
}

export interface Instance {
  id: string,
  name: string,
  vcpu: number,
  memory: number,
  network: string,
  otherTags: string[]
}
