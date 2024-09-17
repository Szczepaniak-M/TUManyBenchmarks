import {Instance} from "../../instance-list/instance.model";
import {groupByToMap, removeUnnecessaryTags} from "./instance.utils";

describe('removeUnnecessaryTags', () => {
  it('should remove matching patterns', () => {
    const instance: Instance = {
      benchmarks: [], id: "", onDemandPrice: 0, spotPrice: 0, memory: 0, name: "", network: "", vcpu: 0,
      tags: ['4 vCPUs', '16 GB Memory', 'High Network', 'Other Tag']
    };

    const result = removeUnnecessaryTags(instance);

    expect(result.tags).not.toContain('4 vCPUs');
    expect(result.tags).not.toContain('16 GB Memory');
    expect(result.tags).not.toContain('High Network');
    expect(result.tags).toEqual(['Other Tag']);

  });

  it('should return an empty array if all tags are removed', () => {
    const instance: Instance = {
      benchmarks: [], id: "", onDemandPrice: 0, spotPrice: 0, memory: 0, name: "", network: "", vcpu: 0,
      tags: ['4 vCPUs', '16 GB Memory', 'High Network']
    };

    const result = removeUnnecessaryTags(instance);

    expect(result.tags).toEqual([]);
  });
});

describe('groupByToMap', () => {
  it('should group elements by a specified key', () => {
    const array = [
      {id: 1, type: 'A'},
      {id: 2, type: 'B'},
      {id: 3, type: 'A'},
      {id: 4, type: 'B'}
    ];

    const result = groupByToMap(array, item => item.type);

    expect(result.get('A')).toEqual([
      {id: 1, type: 'A'},
      {id: 3, type: 'A'}
    ]);
    expect(result.get('B')).toEqual([
      {id: 2, type: 'B'},
      {id: 4, type: 'B'}
    ]);
  });

  it('should handle an empty array', () => {
    const array: any[] = [];

    const result = groupByToMap(array, item => item.type);

    expect(result.size).toBe(0);
  });
});
