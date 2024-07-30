import {TestBed} from "@angular/core/testing";
import {HttpTestingController, provideHttpClientTesting} from "@angular/common/http/testing";
import {InstanceListService} from "./instance-list.service";
import {AuthService} from "../auth/auth.service";
import {Instance} from "./instance.model";
import {of} from "rxjs";
import {provideHttpClient} from "@angular/common/http";
import {environment} from "../../environemnts/environment";

describe("InstanceListService", () => {
  let service: InstanceListService;
  let httpMock: HttpTestingController;
  let mockAuthService: { refreshApiKey: any; };

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

    service = TestBed.inject(InstanceListService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  it("should fetch instances successfully", () => {
    const instances: Instance[] = [
      {id: "id1", name: "t2.micro", tags: ["value1"]},
      {id: "id2", name: "t3.micro", tags: ["value2"]},
    ];

    service.getInstances().subscribe((data) => {
      expect(data).toEqual(instances);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/instance`);
    expect(req.request.method).toBe("GET");
    req.flush(instances);
  });

  it("should retry fetching instances after 401 error and API key refresh", () => {
    const instances: Instance[] = [
      {id: "id1", name: "t2.micro", tags: ["value1"]},
      {id: "id2", name: "t3.micro", tags: ["value2"]},
    ];

    service.getInstances().subscribe((data) => {
      expect(data).toEqual(instances);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/instance`);
    expect(req.request.method).toBe("GET");
    req.flush(null, {status: 401, statusText: "Unauthorized"});

    expect(mockAuthService.refreshApiKey).toHaveBeenCalled();

    const retryReq = httpMock.expectOne(`${environment.apiUrl}/instance`);
    expect(retryReq.request.method).toBe("GET");
    retryReq.flush(instances);
  });

  it("should throw error if non-401 error occurs", () => {
    service.getInstances().subscribe({
      next: () => fail("expected an error, not instances"),
      error: (error) => {
        expect(error.status).toBe(500);
      }
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/instance`);
    expect(req.request.method).toBe("GET");
    req.flush(null, {status: 500, statusText: "Internal Server Error"});
  });
});
