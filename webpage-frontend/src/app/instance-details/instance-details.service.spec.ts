import {TestBed} from "@angular/core/testing";
import {HttpTestingController, provideHttpClientTesting} from "@angular/common/http/testing";
import {InstanceDetailsService} from "./instance-details.service";
import {AuthService} from "../auth/auth.service";
import {of} from "rxjs";
import {environment} from "../../environments/environment";
import {provideHttpClient} from "@angular/common/http";
import {Instance} from "../instance-list/instance.model";

describe("InstanceDetailsService", () => {
  let service: InstanceDetailsService;
  let httpMock: HttpTestingController;
  let mockAuthService: { refreshApiKey: any; };

  const mockInstance: Instance = {
    id: "instance1",
    name: "t2.micro",
    onDemandPrice: 0.1,
    spotPrice: 0.01,
    vcpu: 8,
    memory: 16,
    network: "10 Gbps Network",
    tags: ["8 vCPUs", "10 Gbps Network", "16 GiB Memory", "Other tag"],
    benchmarks: [
      {
        id: "benchmark1",
        name: "Benchmark 1",
        description: "Description 1",
        results: [],
        plots: [{title: "Plot 1", type: "scatter", yaxis: "", series: []}]
      },
      {
        id: "benchmark2",
        name: "Benchmark 2",
        description: "Description 2",
        results: [],
        plots: [{title: "Plot 2", type: "scatter", yaxis: "", series: []}]
      }
    ]
  };

  const expectedInstanceDetails: Instance = {
    id: "instance1",
    name: "t2.micro",
    onDemandPrice: 0.1,
    spotPrice: 0.01,
    vcpu: 8,
    memory: 16,
    network: "10 Gbps Network",
    tags: ["Other tag"],
    benchmarks: [
      {
        id: "benchmark1",
        name: "Benchmark 1",
        description: "Description 1",
        results: [],
        plots: [{title: "Plot 1", type: "scatter", yaxis: "", series: []}]
      },
      {
        id: "benchmark2",
        name: "Benchmark 2",
        description: "Description 2",
        results: [],
        plots: [{title: "Plot 2", type: "scatter", yaxis: "", series: []}]
      }
    ]
  };

  beforeEach(() => {
    mockAuthService = {
      refreshApiKey: jasmine.createSpy("refreshApiKey").and.returnValue(of("new-api-key"))
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        InstanceDetailsService,
        {provide: AuthService, useValue: mockAuthService}
      ]
    });

    service = TestBed.inject(InstanceDetailsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  it("should fetch instance details successfully", () => {
    const instanceName = "t2.micro"
    service.getInstanceDetails(instanceName).subscribe((instanceDetails) => {
      expect(instanceDetails).toEqual(expectedInstanceDetails);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/instance/${instanceName}`);
    expect(req.request.method).toBe("GET");
    req.flush(mockInstance);
  });

  it("should retry fetching instance details after 401 error and API key refresh", () => {
    const instanceName = "t2.micro";
    service.getInstanceDetails(instanceName).subscribe(instanceDetails => {
      expect(instanceDetails).toEqual(expectedInstanceDetails);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/instance/${instanceName}`);
    expect(req.request.method).toBe("GET");
    req.flush(null, {status: 401, statusText: "Unauthorized"});

    expect(mockAuthService.refreshApiKey).toHaveBeenCalled();

    const retryReq = httpMock.expectOne(`${environment.apiUrl}/instance/${instanceName}`);
    expect(retryReq.request.method).toBe("GET");
    retryReq.flush(mockInstance);
  });

  it("should throw error if non-401 error occurs", () => {
    const instanceName = "t2.micro";
    service.getInstanceDetails(instanceName).subscribe({
      next: () => fail("expected an error, not instance details"),
      error: error => expect(error.status).toBe(500)
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/instance/${instanceName}`);
    expect(req.request.method).toBe("GET");
    req.flush(null, {status: 500, statusText: "Internal Server Error"});
  });
});
