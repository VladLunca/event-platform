import { Routes } from '@angular/router';
import {authGuard} from './core/guards/auth.guard';
import {roleGuard} from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component')
      .then(m => m.LoginComponent)
  },
  {
    path: 'unauthorized',
    loadComponent: () => import('./shared/unauthorized/unauthorized.component')
      .then(m => m.UnauthorizedComponent)
  },
 /* {
    path: 'events',
    canActivate: [authGuard],
    loadComponent: () => import('./features/events/event-list/event-list.component')
      .then(m => m.EventListComponent)
  },
  {
    path: 'admin/users',
    canActivate: [roleGuard(['ADMIN'])],
    loadComponent: () => import('./features/admin/create-user/create-user.component')
      .then(m => m.CreateUserComponent)
  },
  {
    path: 'profile',
    canActivate: [roleGuard(['CLIENT'])],
    loadComponent: () => import('./features/client/profile/profile.component')
      .then(m => m.ProfileComponent)
  },*/
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
