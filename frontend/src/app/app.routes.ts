import { Routes, RouterModule } from '@angular/router';
import { Auth } from './pages/auth/auth';
import { BarberDashboard } from './pages/barber/dashboard/BarberDashboard';
import { authGuard } from './services/guard/auth.guard'; 
import { ClientDashboard } from './pages/client/dashboard/ClientDashboard';
import { guestGuard } from './services/guard/landingGuard';

export const routes: Routes = [
  { path: '', redirectTo: 'auth', pathMatch: 'full' },
  
  { 
    path: 'auth', 
    component: Auth, 
    canActivate: [guestGuard] // زِد هادي هنا
  },

  { 
    path: 'barber/dashboard', 
    component: BarberDashboard, 
    canActivate: [authGuard], 
    data: { roles: ['COIFFEUR'] } 
  },
  { 
    path: 'client/dashboard', 
    component: ClientDashboard, 
    canActivate: [authGuard], 
    data: { roles: ['CLIENT'] } 
  },
  { path: '**', redirectTo: 'auth' }
]; // 🚨 Hna fin kan l-ghalat, rddinaha ]; blast }
