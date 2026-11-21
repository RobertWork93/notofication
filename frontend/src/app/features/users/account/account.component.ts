import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { httpErrorMessage } from '../../../core/api/http-error-message';
import { AuthService } from '../../../core/auth/auth.service';
import { NotificationService } from '../../../core/notifications/notification.service';
import { UserService } from '../services/user.service';

/** Lets the logged-in user change their email, name and password. */
@Component({
  selector: 'app-account',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './account.component.html',
  styleUrl: './account.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountComponent {
  private readonly auth = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly currentUser = this.auth.currentUser;
  protected readonly form = inject(NonNullableFormBuilder).group({
    email: [this.currentUser()?.email ?? '', [Validators.required, Validators.email, Validators.maxLength(320)]],
    name: [this.currentUser()?.name ?? '', [Validators.required, Validators.maxLength(100)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });
  protected readonly saving = signal(false);

  protected save(): void {
    const user = this.currentUser();
    if (!user || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const request = this.form.getRawValue();
    this.saving.set(true);
    this.userService
      .update(user.id, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.auth.refreshSession(request.email, request.password, updated);
          this.form.controls.password.reset('');
          this.saving.set(false);
          this.notifications.success('Account saved.');
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.notifications.error(httpErrorMessage(error));
        },
      });
  }
}
