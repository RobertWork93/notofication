import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  {
    path: 'login',
    title: 'Log in',
    loadComponent: () => import('./login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    title: 'Sign up',
    loadComponent: () => import('./register/register.component').then((m) => m.RegisterComponent),
  },
];
