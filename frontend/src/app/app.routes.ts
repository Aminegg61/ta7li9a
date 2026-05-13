import { Routes, RouterModule } from '@angular/router';
import { Auth } from './pages/auth/auth';
import { BarberDashboard } from './pages/barber/dashboard/BarberDashboard';
import { authGuard } from './services/guard/auth.guard'; 
import { ClientDashboard } from './pages/client/dashboard/ClientDashboard';
import { guestGuard } from './services/guard/landingGuard';

// 👇 1. Zedna l-imports dyal l-pages d-Meta hna
import { PrivacyPolicyComponent } from './pages/privacy-policy/privacy-policy.component';
import { TermsComponent } from './pages/terms/terms.component';

export const routes: Routes = [
  { path: '', redirectTo: 'auth', pathMatch: 'full' },
  
  { 
    path: 'auth', 
    component: Auth, 
    canActivate: [guestGuard]
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

  // 👇 2. Zedna l-liens hna (qbel mn l-ster lekher)
  { path: 'privacy-policy', component: PrivacyPolicyComponent },
  { path: 'terms', component: TermsComponent },

  // 🚨 Hada dima k-y-b9a howa l-kher!
  { path: '**', redirectTo: 'auth' }
];
