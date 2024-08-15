import {Instance} from "../../instance-list/instance.model";

const vCPUsPattern = /\d+ vCPUs/;
const memoryPattern = /[\S ]+ Memory/;
const networkPattern = /[\S ]+ Network/;

export function removeUnnecessaryTags(instance: Instance): Instance {
  const vCpuTag = instance.tags.filter(tag => vCPUsPattern.test(tag))[0];
  const memoryTag = instance.tags.filter(tag => memoryPattern.test(tag))[0];
  const networkTag = instance.tags.filter(tag => networkPattern.test(tag))[0];
  const excludeTags = [vCpuTag, memoryTag, networkTag];
  instance.tags = instance.tags.filter(tag => !excludeTags.includes(tag));
  return instance
}

export const groupByToMap = <T, Q>(array: T[], predicate: (value: T, index: number, array: T[]) => Q) =>
  array.reduce((map, value, index, array) => {
    const key = predicate(value, index, array);
    map.get(key)?.push(value) ?? map.set(key, [value]);
    return map;
  }, new Map<Q, T[]>());
