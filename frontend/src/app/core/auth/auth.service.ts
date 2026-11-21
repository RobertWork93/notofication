import { HttpClient, HttpContext, HttpHeaders } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, switchMap, tap } from 'rxjs';

import { User, UserRequest } from '../../shared/models/user.model';
import { API_URL } from '../api/api-url';
import { SKIP_AUTH } from '../interceptors/auth-context';

interface Session {
  readonly token: string;
  readonly user: User;
}

const STORAGE_KEY = 'notificationapp.session';

/**
 * Holds the logged-in user and their HTTP Basic credentials.
 * The session lives in sessionStorage, so it ends when the browser tab closes.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = signal<Session | null>(readStoredSession());

  readonly currentUser = computed(() => this.session()?.user ?? null);
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');

  /** The Authorization header value for the logged-in user, or null when logged out. */
  authorizationHeader(): string | null {
    const session = this.session();
    return session ? `Basic ${session.token}` : null;
  }

  /** Checks the credentials against the backend and starts a session. */
  login(email: string, password: string): Observable<User> {
    const token = encodeCredentials(email, password);
    return this.http
      .get<User>(`${API_URL}/users/me`, {
        headers: new HttpHeaders({ Authorization: `Basic ${token}` }),
        context: new HttpContext().set(SKIP_AUTH, true),
      })
      .pipe(tap((user) => this.startSession(token, user)));
  }

  /** Signs up a new user, then logs them in. */
  register(request: UserRequest): Observable<User> {
    return this.http
      .post<User>(`${API_URL}/users`, request, { context: new HttpContext().set(SKIP_AUTH, true) })
      .pipe(switchMap(() => this.login(request.email, request.password)));
  }

  /** Keeps the session valid after the logged-in user changed their own email or password. */
  refreshSession(email: string, password: string, user: User): void {
    this.startSession(encodeCredentials(email, password), user);
  }

  logout(): void {
    this.session.set(null);
    sessionStorage.removeItem(STORAGE_KEY);
  }

  private startSession(token: string, user: User): void {
    const session: Session = { token, user };
    this.session.set(session);
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  }
}

/** Base64 of "email:password" in UTF-8, as HTTP Basic expects. */
function encodeCredentials(email: string, password: string): string {
  const bytes = new TextEncoder().encode(`${email.trim()}:${password}`);
  return btoa(String.fromCharCode(...bytes));
}

function readStoredSession(): Session | null {
  const stored = sessionStorage.getItem(STORAGE_KEY);
  if (!stored) {
    return null;
  }
  try {
    return JSON.parse(stored) as Session;
  } catch {
    sessionStorage.removeItem(STORAGE_KEY);
    return null;
  }
}
