import { Routes } from '@angular/router';
import { roleGuard } from './core/guards/role.guard';

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
  {
    path: 'events',
    loadComponent: () => import('./features/events/event-list/event-list.component')
      .then(m => m.EventListComponent)
  },
  {
    path: 'events/create',
    canActivate: [roleGuard(['OWNER_EVENT', 'ADMIN'])],
    loadComponent: () => import('./features/events/create-event/create-event.component')
      .then(m => m.CreateEventComponent)
  },
  {
    path: 'events/:id/edit',
    canActivate: [roleGuard(['OWNER_EVENT', 'ADMIN'])],
    loadComponent: () => import('./features/events/edit-event/edit-event.component')
      .then(m => m.EditEventComponent)
  },
  {
    path: 'events/:id',
    loadComponent: () => import('./features/events/event-detail/event-detail.component')
      .then(m => m.EventDetailComponent)
  },
  {
    path: 'profile',
    canActivate: [roleGuard(['CLIENT'])],
    loadComponent: () => import('./features/profile/profile.component')
      .then(m => m.ProfileComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/admin/create-user/create-user.component')
      .then(m => m.CreateUserComponent)
  },
  {
    path: '',
    redirectTo: 'events',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: 'events'
  }
];
