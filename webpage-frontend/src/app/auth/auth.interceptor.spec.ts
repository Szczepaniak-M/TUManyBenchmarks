import {TestBed} from '@angular/core/testing';
import {HTTP_INTERCEPTORS, HttpClient, provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {AuthInterceptor} from './auth.interceptor';

describe('AuthInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  const apiKey = 'mock-api-key';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(
          withInterceptorsFromDi(),
        ),
        provideHttpClientTesting(),
        {provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true}
      ]
    });

    spyOn(localStorage, 'getItem').and.callFake((key: string) => {
      return key === 'public_api_key' ? apiKey : null;
    });

    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should add an Authorization header', () => {
    httpClient.get('/test').subscribe(response => expect(response).toBeTruthy());

    const httpRequest = httpMock.expectOne('/test');

    expect(httpRequest.request.headers.has('X-API-Key')).toEqual(true);
    expect(httpRequest.request.headers.get('X-API-Key')).toBe(apiKey);

    httpRequest.flush({data: 'test'});
  });
});
