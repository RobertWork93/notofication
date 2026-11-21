import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { SKIP_AUTH } from './auth-context';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let router: Router;
  const auth = { authorizationHeader: vi.fn<() => string | null>(), logout: vi.fn() };

  beforeEach(() => {
    auth.authorizationHeader.mockReset().mockReturnValue('Basic abc');
    auth.logout.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => httpTesting.verify());

  it('should add the stored credentials and the XMLHttpRequest marker', () => {
    http.get('/api/v1/alerts').subscribe();
    const request = httpTesting.expectOne('/api/v1/alerts');
    request.flush({});

    expect(request.request.headers.get('Authorization')).toBe('Basic abc');
    expect(request.request.headers.get('X-Requested-With')).toBe('XMLHttpRequest');
  });

  it('should leave requests marked SKIP_AUTH without stored credentials', () => {
    http.get('/api/v1/users/me', { context: new HttpContext().set(SKIP_AUTH, true) }).subscribe();
    const request = httpTesting.expectOne('/api/v1/users/me');
    request.flush({});

    expect(request.request.headers.has('Authorization')).toBe(false);
    expect(request.request.headers.get('X-Requested-With')).toBe('XMLHttpRequest');
  });

  it('should log out and go to login when a request gets 401', () => {
    http.get('/api/v1/alerts').subscribe({ error: () => undefined });
    httpTesting.expectOne('/api/v1/alerts').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(auth.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should not log out on 403', () => {
    http.get('/api/v1/users').subscribe({ error: () => undefined });
    httpTesting.expectOne('/api/v1/users').flush(null, { status: 403, statusText: 'Forbidden' });

    expect(auth.logout).not.toHaveBeenCalled();
  });
});
