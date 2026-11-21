import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router } from '@angular/router';

import { httpErrorMessage } from '../../../core/api/http-error-message';
import { NotificationService } from '../../../core/notifications/notification.service';
import { EVENT_TYPES, EventType } from '../../../shared/models/event-type.model';
import { jsonObjectValidator } from '../../../shared/validators/json-object.validator';
import { CreateEventRequest, EventPayload } from '../models/notification-event.model';
import { EventService } from '../services/event.service';

/** Admin form for reporting a new event. */
@Component({
  selector: 'app-event-create',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule],
  templateUrl: './event-create.component.html',
  styleUrl: './event-create.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EventCreateComponent {
  private readonly eventService = inject(EventService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly eventTypes = EVENT_TYPES;
  protected readonly form = this.formBuilder.group({
    type: this.formBuilder.control<EventType>('breaking_news', Validators.required),
    title: ['', [Validators.required, Validators.maxLength(200)]],
    source: ['', [Validators.required, Validators.maxLength(100)]],
    occurredAt: [toDateTimeLocal(new Date()), Validators.required],
    payload: ['', jsonObjectValidator],
  });
  protected readonly saving = signal(false);

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.eventService
      .create(this.toRequest())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (event) => {
          this.notifications.success('Event created.');
          void this.router.navigate(['/events', event.id]);
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.notifications.error(httpErrorMessage(error));
        },
      });
  }

  private toRequest(): CreateEventRequest {
    const { type, title, source, occurredAt, payload } = this.form.getRawValue();
    const payloadText = payload.trim();
    return {
      type,
      title,
      source,
      occurredAt: new Date(occurredAt).toISOString(),
      ...(payloadText ? { payload: JSON.parse(payloadText) as EventPayload } : {}),
    };
  }
}

/** Formats a date for an <input type="datetime-local">, in local time without seconds. */
function toDateTimeLocal(date: Date): string {
  const pad = (value: number): string => value.toString().padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
