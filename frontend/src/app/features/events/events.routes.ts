import { Routes } from '@angular/router';

import { adminGuard } from '../../core/guards/admin.guard';

export const EVENT_ROUTES: Routes = [
  {
    path: 'new',
    title: 'New event',
    canActivate: [adminGuard],
    loadComponent: () => import('./event-create/event-create.component').then((m) => m.EventCreateComponent),
  },
  {
    path: ':eventId',
    title: 'Event',
    loadComponent: () => import('./event-detail/event-detail.component').then((m) => m.EventDetailComponent),
  },
];
