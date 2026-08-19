import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { Login } from './auth/login/login';
import { EmployeeList } from './employee/employee-list/employee-list';
import { EmployeeDetail } from './employee/employee-detail/employee-detail';
import { AnalyticsDashboard } from './analytics/analytics-dashboard/analytics-dashboard';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: '', component: EmployeeList, canActivate: [authGuard] },
  { path: 'employees/:id', component: EmployeeDetail, canActivate: [authGuard] },
  { path: 'analytics', component: AnalyticsDashboard, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
