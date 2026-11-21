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
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSlideToggleChange, MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';

import { httpErrorMessage } from '../../../core/api/http-error-message';
import { AuthService } from '../../../core/auth/auth.service';
import { NotificationService } from '../../../core/notifications/notification.service';
import { DEFAULT_PAGE_SIZE, PAGE_SIZE_OPTIONS } from '../../../shared/models/page.model';
import { EventTypeLabelPipe } from '../../../shared/pipes/event-type-label.pipe';
import { ConfirmService } from '../../../shared/services/confirm.service';
import { Alert } from '../models/alert.model';
import { ChannelLabelPipe } from '../pipes/channel-label.pipe';
import { AlertService } from '../services/alert.service';

@Component({
  selector: 'app-alert-list',
  imports: [
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSlideToggleModule,
    MatButtonModule,
    EventTypeLabelPipe,
    ChannelLabelPipe,
  ],
  templateUrl: './alert-list.component.html',
  styleUrl: './alert-list.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlertListComponent {
  /** Query parameter: the owner whose alerts are shown. Defaults to the logged-in user. */
  readonly userId = input<string>();

  private readonly alertService = inject(AlertService);
  private readonly auth = inject(AuthService);
  private readonly confirmService = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly columns: string[] = ['name', 'eventType', 'channel', 'enabled', 'actions'];
  protected readonly pageSizeOptions = PAGE_SIZE_OPTIONS;

  protected readonly ownerId = computed(() => this.userId() ?? this.auth.currentUser()?.id ?? '');
  protected readonly isOtherUser = computed(() => this.ownerId() !== this.auth.currentUser()?.id);
  /** Keeps ?userId= on links, so an admin stays on the same user's alerts. */
  protected readonly ownerQueryParams = computed(() => (this.isOtherUser() ? { userId: this.ownerId() } : {}));

  protected readonly alerts = signal<readonly Alert[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(DEFAULT_PAGE_SIZE);
  protected readonly loading = signal(true);

  constructor() {
    effect(() => {
      this.ownerId();
      untracked(() => {
        this.pageIndex.set(0);
        this.load();
      });
    });
  }

  protected changePage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  protected setEnabled(alert: Alert, change: MatSlideToggleChange): void {
    const { name, eventType, channel } = alert;
    this.alertService
      .update(alert.id, alert.userId, { name, eventType, channel, enabled: change.checked })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.alerts.update((alerts) => alerts.map((item) => (item.id === updated.id ? updated : item)));
          this.notifications.success(updated.enabled ? 'Alert enabled.' : 'Alert disabled.');
        },
        error: (error: unknown) => {
          change.source.checked = alert.enabled;
          this.notifications.error(httpErrorMessage(error));
        },
      });
  }

  protected deleteAlert(alert: Alert): void {
    this.confirmService
      .confirm({ title: 'Delete alert', message: `Delete the alert "${alert.name}"?`, confirmLabel: 'Delete' })
      .pipe(
        filter((confirmed) => confirmed),
        switchMap(() => this.alertService.delete(alert.id, alert.userId)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.notifications.success('Alert deleted.');
          this.load();
        },
        error: (error: unknown) => this.notifications.error(httpErrorMessage(error)),
      });
  }

  private load(): void {
    const ownerId = this.ownerId();
    if (!ownerId) {
      return;
    }
    this.loading.set(true);
    this.alertService
      .list(ownerId, this.pageIndex(), this.pageSize())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.alerts.set(page.content);
          this.totalElements.set(page.page.totalElements);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.notifications.error(httpErrorMessage(error));
        },
      });
  }
}
