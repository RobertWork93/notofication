import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { adminGuard } from './admin.guard';
import { authGuard } from './auth.guard';
import { guestGuard } from './guest.guard';

describe('route guards', () => {
  const auth = { isLoggedIn: vi.fn<() => boolean>(), isAdmin: vi.fn<() => boolean>() };
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/alerts/new' } as RouterStateSnapshot;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: AuthService, useValue: auth }] });
  });

  function serialize(result: unknown): string {
    return TestBed.inject(Router).serializeUrl(result as UrlTree);
  }

  it('authGuard should let logged-in users through', () => {
    auth.isLoggedIn.mockReturnValue(true);

    expect(TestBed.runInInjectionContext(() => authGuard(route, state))).toBe(true);
  });

  it('authGuard should send anonymous users to login with a return URL', () => {
    auth.isLoggedIn.mockReturnValue(false);

    const result = TestBed.runInInjectionContext(() => authGuard(route, state));

    expect(serialize(result)).toBe('/login?returnUrl=%2Falerts%2Fnew');
  });

  it('adminGuard should let admins through and send other users to their alerts', () => {
    auth.isAdmin.mockReturnValue(true);
    expect(TestBed.runInInjectionContext(() => adminGuard(route, state))).toBe(true);

    auth.isAdmin.mockReturnValue(false);
    expect(serialize(TestBed.runInInjectionContext(() => adminGuard(route, state)))).toBe('/alerts');
  });

  it('guestGuard should keep logged-in users away from login', () => {
    auth.isLoggedIn.mockReturnValue(true);
    expect(serialize(TestBed.runInInjectionContext(() => guestGuard(route, state)))).toBe('/alerts');

    auth.isLoggedIn.mockReturnValue(false);
    expect(TestBed.runInInjectionContext(() => guestGuard(route, state))).toBe(true);
  });
});
