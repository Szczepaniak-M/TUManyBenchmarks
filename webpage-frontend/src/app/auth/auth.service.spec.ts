import {TestBed} from "@angular/core/testing";
import {HttpTestingController, provideHttpClientTesting} from "@angular/common/http/testing";
import {AuthService} from "./auth.service";
import {BYPASS_INTERCEPTOR} from "./auth.interceptor";
import {provideHttpClient} from "@angular/common/http";
import {environment} from "../../environments/environment";

describe("AuthService", () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const apiUrl = environment.apiUrl;
  const localStorageKey = "public_api_key";
  const mockApiKey = "mock-api-key";

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  describe("getApiKey", () => {
    it("should return stored API key if it exists", done => {
      localStorage.setItem(localStorageKey, mockApiKey);

      service.getApiKey().subscribe(apiKey => {
        expect(apiKey).toBe(mockApiKey);
        done();
      });
    });

    it("should refresh API key if not present in local storage", done => {
      service.getApiKey().subscribe(apiKey => {
        expect(apiKey).toBe(mockApiKey);
        expect(localStorage.getItem("public_api_key")).toBe(mockApiKey);
        done();
      });

      const req = httpMock.expectOne(`${apiUrl}/key`);
      expect(req.request.method).toBe("GET");
      expect(req.request.context.get(BYPASS_INTERCEPTOR)).toBeTrue();
      req.flush({apiKey: mockApiKey});
    });
  });

  describe("refreshApiKey", () => {
    it("should fetch new API key and store it in local storage", done => {
      service.refreshApiKey().subscribe(apiKey => {
        expect(apiKey).toBe(mockApiKey);
        expect(localStorage.getItem(localStorageKey)).toBe(mockApiKey);
        done();
      });

      const req = httpMock.expectOne(`${apiUrl}/key`);
      expect(req.request.method).toBe("GET");
      expect(req.request.context.get(BYPASS_INTERCEPTOR)).toBeTrue();
      req.flush({apiKey: mockApiKey});
    });
  });
});
