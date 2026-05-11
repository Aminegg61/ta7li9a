import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../auth';

export const guestGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    const role = authService.getUserRole();
    if (role === 'CLIENT') {
      router.navigate(['/client/dashboard']);
    } else if (role === 'COIFFEUR') {
      router.navigate(['/barber/dashboard']);
    }
    return false; // بلوكي صفحة الـ Auth حيت ديجا هو Login
  }
  return true; // إلا ما مسجلش، خليه يدخل لـ Auth
};
