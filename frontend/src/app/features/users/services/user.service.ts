import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../../core/api/api-url';
import { Page } from '../../../shared/models/page.model';
import { User, UserRequest } from '../../../shared/models/user.model';

/** Calls the user endpoints. Sign-up lives in AuthService, because it starts a session. */
@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_URL}/users`;

  /** Admin only. */
  list(page: number, size: number): Observable<Page<User>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<User>>(this.baseUrl, { params });
  }

  update(userId: string, request: UserRequest): Observable<User> {
    return this.http.put<User>(this.userUrl(userId), request);
  }

  delete(userId: string): Observable<void> {
    return this.http.delete<void>(this.userUrl(userId));
  }

  private userUrl(userId: string): string {
    return `${this.baseUrl}/${encodeURIComponent(userId)}`;
  }
}
