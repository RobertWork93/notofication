import { Routes } from '@angular/router';

/** Every alert route accepts an optional ?userId= query parameter, so an admin can manage another user's alerts. */
export const ALERT_ROUTES: Routes = [
  {
    path: '',
    title: 'Alerts',
    loadComponent: () => import('./alert-list/alert-list.component').then((m) => m.AlertListComponent),
  },
  {
    path: 'new',
    title: 'New alert',
    loadComponent: () => import('./alert-form/alert-form.component').then((m) => m.AlertFormComponent),
  },
  {
    path: ':alertId/edit',
    title: 'Edit alert',
    loadComponent: () => import('./alert-form/alert-form.component').then((m) => m.AlertFormComponent),
  },
];
