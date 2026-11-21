import { Routes } from '@angular/router';

/** Admin-only routes. The parent route applies authGuard and adminGuard. */
export const USER_ROUTES: Routes = [
  {
    path: '',
    title: 'Users',
    loadComponent: () => import('./user-list/user-list.component').then((m) => m.UserListComponent),
  },
];
