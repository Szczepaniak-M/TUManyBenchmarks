import {TestBed} from '@angular/core/testing';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {AuthService} from './auth.service';
import {BYPASS_INTERCEPTOR} from './auth.interceptor';
import {provideHttpClient} from "@angular/common/http";
import {environment} from "../../environemnts/environment";

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

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
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getApiKey', () => {
    it('should return stored API key if present', (done) => {
      const storedKey = 'stored-api-key';
      localStorage.setItem('public_api_key', storedKey);

      service.getApiKey().subscribe((key) => {
        expect(key).toBe(storedKey);
        done();
      });
    });

    it('should refresh API key if not present in local storage', (done) => {
      const newKey = 'new-api-key';

      service.getApiKey().subscribe((key) => {
        expect(key).toBe(newKey);
        expect(localStorage.getItem('public_api_key')).toBe(newKey);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/key`);
      expect(req.request.method).toBe('GET');
      expect(req.request.context.get(BYPASS_INTERCEPTOR)).toBeTrue();
      req.flush({apiKey: newKey});
    });
  });

  describe('refreshApiKey', () => {
    it('should fetch new API key and store it in local storage', (done) => {
      const newKey = 'new-api-key';

      service.refreshApiKey().subscribe((key) => {
        expect(key).toBe(newKey);
        expect(localStorage.getItem('public_api_key')).toBe(newKey);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/key`);
      expect(req.request.method).toBe('GET');
      expect(req.request.context.get(BYPASS_INTERCEPTOR)).toBeTrue();
      req.flush({apiKey: newKey});
    });
  });
});
