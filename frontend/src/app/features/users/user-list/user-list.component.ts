import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';

import { httpErrorMessage } from '../../../core/api/http-error-message';
import { AuthService } from '../../../core/auth/auth.service';
import { NotificationService } from '../../../core/notifications/notification.service';
import { DEFAULT_PAGE_SIZE, PAGE_SIZE_OPTIONS } from '../../../shared/models/page.model';
import { User } from '../../../shared/models/user.model';
import { ConfirmService } from '../../../shared/services/confirm.service';
import { UserService } from '../services/user.service';

/** Admin view of all users. */
@Component({
  selector: 'app-user-list',
  imports: [RouterLink, DatePipe, MatTableModule, MatPaginatorModule, MatButtonModule],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserListComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly auth = inject(AuthService);
  private readonly confirmService = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly columns: string[] = ['email', 'name', 'role', 'createdAt', 'actions'];
  protected readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  protected readonly currentUserId = this.auth.currentUser()?.id;

  protected readonly users = signal<readonly User[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(DEFAULT_PAGE_SIZE);

  ngOnInit(): void {
    this.load();
  }

  protected changePage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  protected deleteUser(user: User): void {
    this.confirmService
      .confirm({
        title: 'Delete user',
        message: `Delete ${user.email} and all of their alerts? This cannot be undone.`,
        confirmLabel: 'Delete',
      })
      .pipe(
        filter((confirmed) => confirmed),
        switchMap(() => this.userService.delete(user.id)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.notifications.success('User deleted.');
          this.load();
        },
        error: (error: unknown) => this.notifications.error(httpErrorMessage(error)),
      });
  }

  private load(): void {
    this.userService
      .list(this.pageIndex(), this.pageSize())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.users.set(page.content);
          this.totalElements.set(page.page.totalElements);
        },
        error: (error: unknown) => this.notifications.error(httpErrorMessage(error)),
      });
  }
}
