import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { permissionGuard } from './core/guards/permission.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/auth/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: '',
    loadComponent: () => import('./layout/main-layout/main-layout.component').then((m) => m.MainLayoutComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'components',
        loadComponent: () =>
          import('./pages/components-management/component-list/component-list.component').then(
            (m) => m.ComponentListComponent
          ),
        canActivate: [permissionGuard],
        data: { permission: 'COMPONENT_VIEW' }
      },
      {
        path: 'group-categories',
        loadComponent: () =>
          import('./pages/group-category-management/group-category-list/group-category-list.component').then(
            (m) => m.GroupCategoryListComponent
          ),
        canActivate: [permissionGuard],
        data: { permission: 'GROUP_CATEGORY_VIEW' }
      },
      {
        path: 'transactions',
        loadComponent: () =>
          import('./pages/transaction-management/transaction-list/transaction-list.component').then(
            (m) => m.TransactionListComponent
          )
      },
      {
        path: '',
        redirectTo: 'components',
        pathMatch: 'full'
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
