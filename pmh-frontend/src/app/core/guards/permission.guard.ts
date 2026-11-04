import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const permissionGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // If the user is not logged in, return false and let authGuard handle the login redirect.
  if (!authService.isLoggedIn()) {
    return false;
  }

  const requiredPermission = route.data?.['permission'] as string;

  if (!requiredPermission || authService.hasPermission(requiredPermission)) {
    return true;
  }

  return router.createUrlTree(['/components']);
};
