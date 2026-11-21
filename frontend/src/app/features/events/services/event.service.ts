import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../../core/api/api-url';
import { CreateEventRequest, NotificationEvent } from '../models/notification-event.model';

/** Calls the event endpoints. Only admins may create events. */
@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_URL}/events`;

  create(request: CreateEventRequest): Observable<NotificationEvent> {
    return this.http.post<NotificationEvent>(this.baseUrl, request);
  }

  get(eventId: string): Observable<NotificationEvent> {
    return this.http.get<NotificationEvent>(`${this.baseUrl}/${encodeURIComponent(eventId)}`);
  }
}
