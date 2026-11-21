import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { User } from '../../shared/models/user.model';
import { AuthService } from './auth.service';

const JANE: User = {
  id: '0b9f1d2e-3c4a-4b5c-8d6e-7f8091a2b3c4',
  email: 'jane.doe@example.com',
  name: 'Jane Doe',
  role: 'USER',
  createdAt: '2026-11-21T15:00:00Z',
  updatedAt: '2026-11-21T15:00:00Z',
};

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should log in with HTTP Basic credentials and start a session', () => {
    let loggedIn: User | undefined;

    service.login('jane.doe@example.com', 'secret123').subscribe((user) => (loggedIn = user));
    const request = http.expectOne('/api/v1/users/me');
    request.flush(JANE);

    const expectedHeader = `Basic ${btoa('jane.doe@example.com:secret123')}`;
    expect(request.request.headers.get('Authorization')).toBe(expectedHeader);
    expect(loggedIn).toEqual(JANE);
    expect(service.isLoggedIn()).toBe(true);
    expect(service.isAdmin()).toBe(false);
    expect(service.authorizationHeader()).toBe(expectedHeader);
  });

  it('should not start a session when the credentials are wrong', () => {
    service.login('jane.doe@example.com', 'wrong').subscribe({ error: () => undefined });
    http.expectOne('/api/v1/users/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(service.isLoggedIn()).toBe(false);
    expect(service.authorizationHeader()).toBeNull();
  });

  it('should sign up and then log in with the same credentials', () => {
    let loggedIn: User | undefined;

    service
      .register({ email: 'jane.doe@example.com', name: 'Jane Doe', password: 'secret123' })
      .subscribe((user) => (loggedIn = user));
    const signUp = http.expectOne({ method: 'POST', url: '/api/v1/users' });
    expect(signUp.request.body).toEqual({ email: 'jane.doe@example.com', name: 'Jane Doe', password: 'secret123' });
    signUp.flush(JANE);
    http.expectOne('/api/v1/users/me').flush(JANE);

    expect(loggedIn).toEqual(JANE);
    expect(service.currentUser()).toEqual(JANE);
  });

  it('should encode non-ASCII credentials as UTF-8', () => {
    service.login('jane.doe@example.com', 'pässwörd1').subscribe();
    const request = http.expectOne('/api/v1/users/me');
    request.flush(JANE);

    const encoded = request.request.headers.get('Authorization')?.replace('Basic ', '') ?? '';
    const decoded = new TextDecoder().decode(Uint8Array.from(atob(encoded), (char) => char.charCodeAt(0)));
    expect(decoded).toBe('jane.doe@example.com:pässwörd1');
  });

  it('should forget the session on logout', () => {
    service.login('jane.doe@example.com', 'secret123').subscribe();
    http.expectOne('/api/v1/users/me').flush(JANE);

    service.logout();

    expect(service.isLoggedIn()).toBe(false);
    expect(sessionStorage.length).toBe(0);
  });
});
