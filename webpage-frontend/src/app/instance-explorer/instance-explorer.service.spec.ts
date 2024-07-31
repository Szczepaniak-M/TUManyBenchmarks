import {TestBed} from "@angular/core/testing";
import {HttpTestingController, provideHttpClientTesting} from "@angular/common/http/testing";
import {InstanceDetailsService} from "./instance-explorer.service";
import {AuthService} from "../auth/auth.service";
import {environment} from "../../environemnts/environment";
import {InstanceExplorerRequest, InstanceExplorerResponse} from "./instance-explorer.model";
import {of} from "rxjs";
import {provideHttpClient} from "@angular/common/http";

describe("InstanceDetailsService", () => {
  let service: InstanceDetailsService;
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

  it("should execute query and return results", () => {
    const mockRequest: InstanceExplorerRequest = {
      aggregationStages: ["{'stage': 'test'}"],
      partialResults: false
    };
    const mockResponse: InstanceExplorerResponse = {
      successfulQuires: 1,
      totalQueries: 1,
      results: ['{"key": "value"}'],
      error: undefined
    };

    service.executeQuery(mockRequest.aggregationStages, mockRequest.partialResults).subscribe(response => {
      expect(response).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/explorer`);
    expect(req.request.method).toBe("POST");
    req.flush(mockResponse);
  });

  it("should retry on 401 error and refresh API key", () => {
    const mockRequest: InstanceExplorerRequest = {
      aggregationStages: ["{'stage': 'test'}"],
      partialResults: false
    };
    const mockResponse: InstanceExplorerResponse = {
      successfulQuires: 1,
      totalQueries: 1,
      results: ['{"key": "value"}'],
      error: undefined
    };

    service.executeQuery(mockRequest.aggregationStages, mockRequest.partialResults).subscribe(response => {
      expect(response).toEqual(mockResponse);
    });

    const firstRequest = httpMock.expectOne(`${environment.apiUrl}/explorer`);
    expect(firstRequest.request.method).toBe("POST");
    firstRequest.flush({}, {status: 401, statusText: "Unauthorized"});

    expect(mockAuthService.refreshApiKey).toHaveBeenCalled();

    const secondRequest = httpMock.expectOne(`${environment.apiUrl}/explorer`);
    expect(secondRequest.request.method).toBe("POST");
    secondRequest.flush(mockResponse);
  });

  it("should throw an error on non-401 error", () => {
    const mockRequest: InstanceExplorerRequest = {
      aggregationStages: ["{'stage': 'test'}"],
      partialResults: false
    };

    service.executeQuery(mockRequest.aggregationStages, mockRequest.partialResults).subscribe({
      next: () => fail("expected an error, not results"),
      error: error => expect(error.status).toBe(500)
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/explorer`);
    expect(req.request.method).toBe("POST");
    req.flush({}, {status: 500, statusText: "Server Error"});
  });
});
