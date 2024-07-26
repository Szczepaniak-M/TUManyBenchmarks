import {Instance, InstanceDto} from "../instance-list/instance.model";


const vCPUsPattern = /\d+ vCPUs/;
const memoryPattern = /[\S ]+ Memory/;
const networkPattern = /[\S ]+ Network/;

export function convertInstanceDtoToInstance(instance: InstanceDto): Instance {
  const vCpuTag = instance.tags.filter(tag => vCPUsPattern.test(tag))[0]
  const memoryTag = instance.tags.filter(tag => memoryPattern.test(tag))[0]
  const networkTag = instance.tags.filter(tag => networkPattern.test(tag))[0]
  const excludeTags = [vCpuTag, memoryTag, networkTag];
  const otherTags = instance.tags.filter(tag => !excludeTags.includes(tag))
  const vCpu = Number(vCpuTag.split(" ")[0])
  const memory = Number(memoryTag.split(" ")[0])
  return {
    id: instance.id,
    name: instance.name,
    vcpu: vCpu,
    memory: memory,
    network: networkTag,
    otherTags: otherTags
  }
}
