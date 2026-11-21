import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AlertService } from './alert.service';

describe('AlertService', () => {
  let service: AlertService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AlertService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should list a user’s alerts with paging parameters', () => {
    service.list('user-1', 2, 50).subscribe();

    const request = http.expectOne((req) => req.url === '/api/v1/alerts');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('userId')).toBe('user-1');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('50');
    request.flush({ content: [], page: { size: 50, number: 2, totalElements: 0, totalPages: 0 } });
  });

  it('should create an alert with the owner in the body', () => {
    const body = { userId: 'user-1', name: 'Quakes', eventType: 'natural_disaster', channel: 'email', enabled: true } as const;

    service.create(body).subscribe();

    const request = http.expectOne({ method: 'POST', url: '/api/v1/alerts' });
    expect(request.request.body).toEqual(body);
    request.flush({});
  });

  it('should address a single alert together with its owner', () => {
    service.update('alert-1', 'user-1', { name: 'News', eventType: 'breaking_news', channel: 'slack', enabled: false }).subscribe();
    http.expectOne({ method: 'PUT', url: '/api/v1/alerts/alert-1/users/user-1' }).flush({});

    service.delete('alert-1', 'user-1').subscribe();
    http.expectOne({ method: 'DELETE', url: '/api/v1/alerts/alert-1/users/user-1' }).flush(null);
  });
});
