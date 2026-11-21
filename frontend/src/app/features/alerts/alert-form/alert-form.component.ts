import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { httpErrorMessage } from '../../../core/api/http-error-message';
import { AuthService } from '../../../core/auth/auth.service';
import { NotificationService } from '../../../core/notifications/notification.service';
import { EVENT_TYPES, EventType } from '../../../shared/models/event-type.model';
import { Alert, CHANNELS, Channel } from '../models/alert.model';
import { AlertService } from '../services/alert.service';

/** Creates a new alert, or edits one when the route has an alertId. */
@Component({
  selector: 'app-alert-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatButtonModule,
  ],
  templateUrl: './alert-form.component.html',
  styleUrl: './alert-form.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlertFormComponent {
  /** Route parameter: set when editing an existing alert. */
  readonly alertId = input<string>();
  /** Query parameter: the owner of the alert. Defaults to the logged-in user. */
  readonly userId = input<string>();

  private readonly alertService = inject(AlertService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly eventTypes = EVENT_TYPES;
  protected readonly channels = CHANNELS;

  protected readonly isEdit = computed(() => this.alertId() !== undefined);
  protected readonly ownerId = computed(() => this.userId() ?? this.auth.currentUser()?.id ?? '');
  protected readonly backQueryParams = computed(() =>
    this.ownerId() !== this.auth.currentUser()?.id ? { userId: this.ownerId() } : {},
  );

  protected readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    eventType: this.formBuilder.control<EventType>('natural_disaster', Validators.required),
    channel: this.formBuilder.control<Channel>('email', Validators.required),
    enabled: [true],
  });
  protected readonly saving = signal(false);

  constructor() {
    effect(() => {
      const alertId = this.alertId();
      if (alertId) {
        untracked(() => this.loadAlert(alertId));
      }
    });
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.saveRequest()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notifications.success(this.isEdit() ? 'Alert saved.' : 'Alert created.');
          void this.router.navigate(['/alerts'], { queryParams: this.backQueryParams() });
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.notifications.error(httpErrorMessage(error));
        },
      });
  }

  private saveRequest(): Observable<Alert> {
    const fields = this.form.getRawValue();
    const alertId = this.alertId();
    return alertId
      ? this.alertService.update(alertId, this.ownerId(), fields)
      : this.alertService.create({ ...fields, userId: this.ownerId() });
  }

  private loadAlert(alertId: string): void {
    this.alertService
      .get(alertId, this.ownerId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ name, eventType, channel, enabled }) => this.form.setValue({ name, eventType, channel, enabled }),
        error: (error: unknown) => {
          this.notifications.error(httpErrorMessage(error));
          void this.router.navigate(['/alerts'], { queryParams: this.backQueryParams() });
        },
      });
  }
}
