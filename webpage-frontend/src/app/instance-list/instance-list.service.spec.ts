import {TestBed} from "@angular/core/testing";
import {HttpTestingController, provideHttpClientTesting} from "@angular/common/http/testing";
import {InstanceListService} from "./instance-list.service";
import {AuthService} from "../auth/auth.service";
import {BenchmarkDetails, BenchmarkStatistics, Instance} from "./instance.model";
import {of} from "rxjs";
import {provideHttpClient} from "@angular/common/http";
import {environment} from "../../environments/environment";

describe("InstanceListService", () => {
  let instanceListService: InstanceListService;
  let httpMock: HttpTestingController;
  let mockAuthService: { refreshApiKey: any; };

  const mockApiUrl = environment.apiUrl;

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

  describe("getInstances", () => {
    const mockInstances: Instance[] = [
      {
        id: "id1", onDemandPrice: 0.01, spotPrice: 0.001, name: "t2.micro", vcpu: 4, memory: 16, network: "10 Gib Network",
        tags: ["4 vCPUs", "16 GiB Memory", "10 Gib Network", "Additional Tag"], benchmarks: []
      },
      {
        id: "id2", onDemandPrice: 0.02, spotPrice: 0.002, name: "t2.small", vcpu: 8, memory: 16, network: "10 Gib Network",
        tags: ["8 vCPUs", "16 GiB Memory", "10 Gib Network", "Additional Tag"], benchmarks: []
      },
      {
        id: "id3", onDemandPrice: 0.03, spotPrice: 0.003, name: "t2.nano", vcpu: 16, memory: 16, network: "10 Gib Network",
        tags: ["16 vCPUs", "16 GiB Memory", "10 Gib Network", "Additional Tag"], benchmarks: []
      }
    ];

    const expectedInstances: Instance[] = [
      {
        id: "id1", onDemandPrice: 0.01, spotPrice: 0.001, name: "t2.micro", vcpu: 4, memory: 16, network: "10 Gib Network",
        tags: ["Additional Tag"], benchmarks: [],
      },
      {
        id: "id2", onDemandPrice: 0.02, spotPrice: 0.002, name: "t2.small", vcpu: 8, memory: 16, network: "10 Gib Network",
        tags: ["Additional Tag"], benchmarks: []
      },
      {
        id: "id3", onDemandPrice: 0.03, spotPrice: 0.003, name: "t2.nano", vcpu: 16, memory: 16, network: "10 Gib Network",
        tags: ["Additional Tag"], benchmarks: []
      }
    ];

    it("should fetch instances successfully", () => {
      instanceListService.getInstances().subscribe((data) => {
        expect(data).toEqual(expectedInstances);
      });

      const req = httpMock.expectOne(`${mockApiUrl}/instance`);
      expect(req.request.method).toBe("GET");
      req.flush(mockInstances);
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
      retryReq.flush(mockInstances);
    });

    it("should throw error if non-401 error occurs while fetching instances", () => {
      instanceListService.getInstances().subscribe({
        next: () => fail("expected an error, not instances"),
        error: error => expect(error.status).toBe(500)
      });

      const req = httpMock.expectOne(`${mockApiUrl}/instance`);
      expect(req.request.method).toBe("GET");
      req.flush(null, {status: 500, statusText: "Internal Server Error"});
    });
  });


  describe("getBenchmarks", () => {
    const benchmarks: BenchmarkDetails[] = [
      {
        id: "id1", name: "benchmark1", description: "benchmark1",
        instanceTypes: ["t2.micro", "t2.nano"], instanceTags: [],
        seriesX: ["line1X"], seriesY: ["line1Y"], seriesOther: []
      },
      {
        id: "id2", name: "benchmark2", description: "benchmark2",
        instanceTypes: [], instanceTags: [["8 vCPUs", "16 GiB Memory"], ["4 vCPUs", "8 GiB Memory"]],
        seriesX: [], seriesY: ["scatter2"], seriesOther: []
      },
    ]

    it("should fetch benchmarks successfully", () => {
      instanceListService.getBenchmarks().subscribe((data) => {
        expect(data).toEqual(benchmarks);
      });

      const req = httpMock.expectOne(`${mockApiUrl}/benchmark`);
      expect(req.request.method).toBe("GET");
      req.flush(benchmarks);
    });

    it("should retry fetching benchmarks after 401 error and API key refresh", () => {
      instanceListService.getBenchmarks().subscribe((data) => {
        expect(data).toEqual(benchmarks);
      });

      const req = httpMock.expectOne(`${mockApiUrl}/benchmark`);
      expect(req.request.method).toBe("GET");
      req.flush(null, {status: 401, statusText: "Unauthorized"});

      expect(mockAuthService.refreshApiKey).toHaveBeenCalled();

      const retryReq = httpMock.expectOne(`${mockApiUrl}/benchmark`);
      expect(retryReq.request.method).toBe("GET");
      retryReq.flush(benchmarks);
    });

    it("should throw error if non-401 error occurs while fetching benchmarks", () => {
      instanceListService.getBenchmarks().subscribe({
        next: () => fail("expected an error, not benchmarks"),
        error: error => expect(error.status).toBe(500)
      });

      const req = httpMock.expectOne(`${mockApiUrl}/benchmark`);
      expect(req.request.method).toBe("GET");
      req.flush(null, {status: 500, statusText: "Internal Server Error"});
    });
  });

  describe("getStatistics", () => {
    const statistics: BenchmarkStatistics[] = [
      {
        instanceId: "instance1", benchmarkId: "benchmark1", series: "series1",
        min: 1.0000001, max: 5.0000001, avg: 3.0000001, median: 2.0000001,
      },
      {
        instanceId: "instance1", benchmarkId: "benchmark1", series: "series2",
        min: 2.0000001, max: 6.0000001, avg: 4.0000001, median: 3.0000001,
      }
    ]

    const expectedStatistics: BenchmarkStatistics[] = [
      {
        instanceId: "instance1", benchmarkId: "benchmark1", series: "series1",
        min: 1, max: 5, avg: 3, median: 2,
      },
      {
        instanceId: "instance1", benchmarkId: "benchmark1", series: "series2",
        min: 2, max: 6, avg: 4, median: 3,
      }
    ]

    it("should fetch benchmark statistics successfully", () => {
      instanceListService.getStatistics().subscribe((data) => {
        expect(data).toEqual(expectedStatistics);
      });

      const req = httpMock.expectOne(`${mockApiUrl}/statistics`);
      expect(req.request.method).toBe("GET");
      req.flush(statistics);
    });

    it("should retry fetching benchmark statistics after 401 error and API key refresh", () => {
      instanceListService.getStatistics().subscribe((data) => {
        expect(data).toEqual(expectedStatistics);
      });

      const req = httpMock.expectOne(`${mockApiUrl}/statistics`);
      expect(req.request.method).toBe("GET");
      req.flush(null, {status: 401, statusText: "Unauthorized"});

      expect(mockAuthService.refreshApiKey).toHaveBeenCalled();

      const retryReq = httpMock.expectOne(`${mockApiUrl}/statistics`);
      expect(retryReq.request.method).toBe("GET");
      retryReq.flush(statistics);
    });

    it("should throw error if non-401 error occurs while fetching benchmarks statistics", () => {
      instanceListService.getStatistics().subscribe({
        next: () => fail("expected an error, not statistics"),
        error: error => expect(error.status).toBe(500)
      });

      const req = httpMock.expectOne(`${mockApiUrl}/statistics`);
      expect(req.request.method).toBe("GET");
      req.flush(null, {status: 500, statusText: "Internal Server Error"});
    });
  });
});
