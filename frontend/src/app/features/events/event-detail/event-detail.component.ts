import { DatePipe, JsonPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';

import { httpErrorMessage } from '../../../core/api/http-error-message';
import { EventTypeLabelPipe } from '../../../shared/pipes/event-type-label.pipe';
import { NotificationEvent } from '../models/notification-event.model';
import { EventService } from '../services/event.service';

/** Shows one event, read by its id from the route. */
@Component({
  selector: 'app-event-detail',
  imports: [MatCardModule, DatePipe, JsonPipe, EventTypeLabelPipe],
  templateUrl: './event-detail.component.html',
  styleUrl: './event-detail.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EventDetailComponent {
  /** Route parameter. */
  readonly eventId = input.required<string>();

  private readonly eventService = inject(EventService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly event = signal<NotificationEvent | null>(null);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    effect(() => {
      const eventId = this.eventId();
      untracked(() => this.load(eventId));
    });
  }

  private load(eventId: string): void {
    this.errorMessage.set(null);
    this.eventService
      .get(eventId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (event) => this.event.set(event),
        error: (error: unknown) => this.errorMessage.set(httpErrorMessage(error)),
      });
  }
}
