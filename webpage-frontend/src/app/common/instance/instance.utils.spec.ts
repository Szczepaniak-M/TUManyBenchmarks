import {Instance, InstanceDto} from "../../instance-list/instance.model";
import {convertInstanceDtoToInstance} from "./instance.utils";


describe("convertInstanceDtoToInstance", () => {

  it("should correctly convert InstanceDto with all tags present", () => {
    const instanceDto: InstanceDto = {
      id: "1",
      name: "Test Instance",
      tags: ["4 vCPUs", "16 GiB Memory", "1 Network", "Additional Tag 1", "Additional Tag 2"]
    };

    const expectedInstance: Instance = {
      id: "1",
      name: "Test Instance",
      vcpu: 4,
      memory: 16,
      network: "1 Network",
      otherTags: ["Additional Tag 1", "Additional Tag 2"]
    };

    expect(convertInstanceDtoToInstance(instanceDto)).toEqual(expectedInstance);
  });
});
