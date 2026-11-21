import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../../core/api/api-url';
import { Page } from '../../../shared/models/page.model';
import { Alert, CreateAlertRequest, UpdateAlertRequest } from '../models/alert.model';

/** Calls the alert endpoints. A single alert is always addressed together with its owner. */
@Injectable({ providedIn: 'root' })
export class AlertService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_URL}/alerts`;

  list(userId: string, page: number, size: number): Observable<Page<Alert>> {
    const params = new HttpParams().set('userId', userId).set('page', page).set('size', size);
    return this.http.get<Page<Alert>>(this.baseUrl, { params });
  }

  get(alertId: string, userId: string): Observable<Alert> {
    return this.http.get<Alert>(this.alertUrl(alertId, userId));
  }

  create(request: CreateAlertRequest): Observable<Alert> {
    return this.http.post<Alert>(this.baseUrl, request);
  }

  update(alertId: string, userId: string, request: UpdateAlertRequest): Observable<Alert> {
    return this.http.put<Alert>(this.alertUrl(alertId, userId), request);
  }

  delete(alertId: string, userId: string): Observable<void> {
    return this.http.delete<void>(this.alertUrl(alertId, userId));
  }

  private alertUrl(alertId: string, userId: string): string {
    return `${this.baseUrl}/${encodeURIComponent(alertId)}/users/${encodeURIComponent(userId)}`;
  }
}
