import { Routes } from '@angular/router';

import { adminGuard } from './core/guards/admin.guard';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'alerts' },
  {
    path: '',
    canActivate: [guestGuard],
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'alerts',
    canActivate: [authGuard],
    loadChildren: () => import('./features/alerts/alerts.routes').then((m) => m.ALERT_ROUTES),
  },
  {
    path: 'events',
    canActivate: [authGuard],
    loadChildren: () => import('./features/events/events.routes').then((m) => m.EVENT_ROUTES),
  },
  {
    path: 'users',
    canActivate: [authGuard, adminGuard],
    loadChildren: () => import('./features/users/users.routes').then((m) => m.USER_ROUTES),
  },
  {
    path: 'account',
    canActivate: [authGuard],
    title: 'My account',
    loadComponent: () =>
      import('./features/users/account/account.component').then((m) => m.AccountComponent),
  },
  { path: '**', redirectTo: 'alerts' },
];
