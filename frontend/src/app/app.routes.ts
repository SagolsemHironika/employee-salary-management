import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';

// Every route is lazy. Analytics alone pulls ~160kB of charting that most
// sessions never open, and the sign-in screen used to pay for the entire
// authenticated app before the user had even typed a password.
export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./auth/login/login').then((m) => m.Login) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./employee/employee-list/employee-list').then((m) => m.EmployeeList)
  },
  {
    path: 'employees/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./employee/employee-detail/employee-detail').then((m) => m.EmployeeDetail)
  },
  {
    path: 'analytics',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./analytics/analytics-dashboard/analytics-dashboard').then((m) => m.AnalyticsDashboard)
  },
  { path: '**', redirectTo: '' }
];
