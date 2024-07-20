import {TestBed} from '@angular/core/testing';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {InstanceDetailsService} from './instance-details.service';
import {AuthService} from '../auth/auth.service';
import {InstanceDetails} from './instance-details.model';
import {of} from 'rxjs';
import {environment} from "../../environemnts/environment";
import {provideHttpClient} from "@angular/common/http";

describe('InstanceDetailsService', () => {
  let service: InstanceDetailsService;
  let httpMock: HttpTestingController;
  let mockAuthService: { refreshApiKey: any; };

  beforeEach(() => {
    mockAuthService = {
      refreshApiKey: jasmine.createSpy('refreshApiKey').and.returnValue(of('new-api-key'))
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

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch instance details successfully', () => {
    const instanceName = 't2.micro';
    const instanceDetails: InstanceDetails = {id: 'id1', name: instanceName, tags: ['tag1'], benchmarks: []};

    service.getInstanceDetails(instanceName).subscribe((details) => {
      expect(details).toEqual(instanceDetails);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/instance/${instanceName}`);
    expect(req.request.method).toBe('GET');
    req.flush(instanceDetails);
  });

  it('should retry fetching instance details after 401 error and API key refresh', () => {
    const instanceName = 't2.micro';
    const instanceDetails: InstanceDetails = {id: 'id1', name: instanceName, tags: ['tag1'], benchmarks: []};

    service.getInstanceDetails(instanceName).subscribe((details) => {
      expect(details).toEqual(instanceDetails);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/instance/${instanceName}`);
    expect(req.request.method).toBe('GET');
    req.flush(null, {status: 401, statusText: 'Unauthorized'});

    expect(mockAuthService.refreshApiKey).toHaveBeenCalled();

    const retryReq = httpMock.expectOne(`${environment.apiUrl}/instance/${instanceName}`);
    expect(retryReq.request.method).toBe('GET');
    retryReq.flush(instanceDetails);
  });

  it('should throw error if non-401 error occurs', () => {
    const instanceName = 't2.micro';

    service.getInstanceDetails(instanceName).subscribe({
      next: () => fail('expected an error, not instance details'),
      error: (error) => {
        expect(error.status).toBe(500);
      }
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/instance/${instanceName}`);
    expect(req.request.method).toBe('GET');
    req.flush(null, {status: 500, statusText: 'Internal Server Error'});
  });
});
