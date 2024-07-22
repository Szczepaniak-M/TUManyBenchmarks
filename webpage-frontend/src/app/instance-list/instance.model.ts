export interface InstanceDto {
  id: string;
  name: string;
  tags: string[];
}

export interface Instance {
  id: string;
  name: string;
  vCpu: string;
  network: string;
  memory: string;
  otherTags: string[];
}
