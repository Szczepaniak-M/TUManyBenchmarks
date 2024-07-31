import {TestBed} from "@angular/core/testing";
import {
  HTTP_INTERCEPTORS,
  HttpClient,
  HttpContext,
  provideHttpClient,
  withInterceptorsFromDi
} from "@angular/common/http";
import {HttpTestingController, provideHttpClientTesting} from "@angular/common/http/testing";
import {AuthInterceptor, BYPASS_INTERCEPTOR} from "./auth.interceptor";
import {AuthService} from "./auth.service";
import {of} from "rxjs";

describe("AuthInterceptor", () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  let mockAuthService: { getApiKey: any };
  const apiKey = "mock-api-key";

  beforeEach(() => {
    mockAuthService = {
      getApiKey: jasmine.createSpy("getApiKey").and.returnValue(of(apiKey))
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(
          withInterceptorsFromDi(),
        ),
        provideHttpClientTesting(),
        {provide: AuthService, useValue: mockAuthService},
        {provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true}
      ]
    });

    spyOn(localStorage, "getItem").and.callFake(key => {
      return key === "public_api_key" ? apiKey : null;
    });

    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it("should add X-API-Key header", () => {
    httpClient.get("/test")
      .subscribe(response => expect(response).toBeTruthy());

    const httpRequest = httpMock.expectOne("/test");

    expect(httpRequest.request.headers.has("X-API-Key")).toEqual(true);
    expect(httpRequest.request.headers.get("X-API-Key")).toBe(apiKey);
    httpRequest.flush({});
  });

  it("should bypass the interceptor when BYPASS_INTERCEPTOR is true", () => {
    httpClient.get("/test", {context: new HttpContext().set(BYPASS_INTERCEPTOR, true)})
      .subscribe(response => expect(response).toBeTruthy());

    const httpRequest = httpMock.expectOne("/test");

    expect(httpRequest.request.headers.has("X-API-Key")).toBeFalse();
    httpRequest.flush({});
  });
});
