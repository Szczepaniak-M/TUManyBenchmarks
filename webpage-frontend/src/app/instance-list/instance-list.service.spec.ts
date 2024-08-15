import {TestBed} from "@angular/core/testing";
import {HttpTestingController, provideHttpClientTesting} from "@angular/common/http/testing";
import {InstanceListService} from "./instance-list.service";
import {AuthService} from "../auth/auth.service";
import {Instance} from "./instance.model";
import {of} from "rxjs";
import {provideHttpClient} from "@angular/common/http";
import {environment} from "../../environemnts/environment";

describe("InstanceListService", () => {
  let instanceListService: InstanceListService;
  let httpMock: HttpTestingController;
  let mockAuthService: { refreshApiKey: any; };

  const mockApiUrl = environment.apiUrl;
  const mockInstancesDto: Instance[] = [
    {
      id: "id1", name: "t2.micro", tags: ["4 vCPUs", "16 GiB Memory", "10 Gib Network", "Additional Tag"],
      vcpu: 0, memory: 0, network: "", benchmarks: []
    },
    {
      id: "id2", name: "t2.small", tags: ["8 vCPUs", "16 GiB Memory", "10 Gib Network", "Additional Tag"],
      vcpu: 0, memory: 0, network: "", benchmarks: []
    },
    {
      id: "id3", name: "t2.nano", tags: ["16 vCPUs", "16 GiB Memory", "10 Gib Network", "Additional Tag"],
      vcpu: 0, memory: 0, network: "", benchmarks: []
    }
  ];

  const expectedInstances: Instance[] = mockInstancesDto
    // .map(convertInstanceDtoToInstance);

  beforeEach(() => {
    mockAuthService = {
      refreshApiKey: jasmine.createSpy("refreshApiKey").and.returnValue(of("new-api-key"))
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        InstanceListService,
        {provide: AuthService, useValue: mockAuthService}
      ]
    });

    instanceListService = TestBed.inject(InstanceListService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it("should be created", () => {
    expect(instanceListService).toBeTruthy();
  });

  it("should fetch instances successfully", () => {
    instanceListService.getInstances().subscribe((data) => {
      expect(data).toEqual(expectedInstances);
    });

    const req = httpMock.expectOne(`${mockApiUrl}/instance`);
    expect(req.request.method).toBe("GET");
    req.flush(mockInstancesDto);
  });

  it("should retry fetching instances after 401 error and API key refresh", () => {
    instanceListService.getInstances().subscribe((data) => {
      expect(data).toEqual(expectedInstances);
    });

    const req = httpMock.expectOne(`${mockApiUrl}/instance`);
    expect(req.request.method).toBe("GET");
    req.flush(null, {status: 401, statusText: "Unauthorized"});

    expect(mockAuthService.refreshApiKey).toHaveBeenCalled();

    const retryReq = httpMock.expectOne(`${mockApiUrl}/instance`);
    expect(retryReq.request.method).toBe("GET");
    retryReq.flush(mockInstancesDto);
  });

  it("should throw error if non-401 error occurs", () => {
    instanceListService.getInstances().subscribe({
      next: () => fail("expected an error, not instances"),
      error: error => expect(error.status).toBe(500)
    });

    const req = httpMock.expectOne(`${mockApiUrl}/instance`);
    expect(req.request.method).toBe("GET");
    req.flush(null, {status: 500, statusText: "Internal Server Error"});
  });
});
