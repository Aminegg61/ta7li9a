import { Component, OnInit, OnDestroy,NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BarberService } from '../../../services/barber.service';
import { WebsocketService } from '../../../services/websocket.service';
import { AppointmentService } from '../../../services/appointment.service';
import { ServiceCatalogService } from '../../../services/service-catalog.service';
import { AppointmentRequestDTO, AppointmentResponseDTO, BarberSearchDto, ServiceResponseDTO, User } from '../../../models/interfaces';
import { Subscription } from 'rxjs';
import { ChangeDetectorRef } from '@angular/core';
import { AuthService } from '../../../services/auth';

@Component({
  selector: 'app-client-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="min-h-screen bg-neutral-950 text-white font-sans">
      <!-- HEADER -->
      <header class="sticky top-0 z-40 bg-black/80 backdrop-blur-md border-b border-neutral-800 px-6 py-4 flex items-center justify-between">
        <div class="flex items-center gap-4">
          <button (click)="drawerOpen = true" class="p-2 -ml-2 text-neutral-400 hover:text-white transition-colors">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
            </svg>
          </button>
          <div>
            <h1 class="text-xl font-black italic uppercase tracking-tighter">TA7LI9A</h1>
            <p class="text-[10px] text-yellow-500 font-black uppercase tracking-widest">Client Portal</p>
          </div>
        </div>
        <span class="text-xs font-bold bg-neutral-900 px-3 py-1.5 rounded-full border border-neutral-800">
          {{ currentUser?.firstName }} {{ currentUser?.lastName }}
        </span>
        <button (click)="logout()" class="text-xs font-bold text-red-500 hover:text-red-400 uppercase tracking-widest">
          Logout
        </button>
      </header>

      <!-- DRAWER MODAL -->
      <div *ngIf="drawerOpen" class="fixed inset-0 z-50 flex">
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" (click)="drawerOpen = false"></div>
        <div class="relative w-80 bg-neutral-900 h-full border-r border-neutral-800 shadow-2xl flex flex-col transform transition-transform">
          <div class="p-6 border-b border-neutral-800 flex justify-between items-center bg-neutral-950">
            <h2 class="font-black text-lg uppercase tracking-tight italic">Menu</h2>
            <button (click)="drawerOpen = false" class="text-neutral-500 hover:text-white">✕</button>
          </div>
          <div class="p-4 flex-1 space-y-2"> <!-- Zedt space-y-2 hna bach y-tferqou -->
            <button (click)="openSearchModal()" class="w-full text-left px-4 py-3 bg-neutral-950 hover:bg-neutral-800 rounded-xl font-bold transition-all text-sm flex items-center gap-3">
              <span>🔍</span> Search All Barbers
            </button>
            
            <!-- 🔥 BOUTON JDIDA DYAL REQUESTS 🔥 -->
            <button (click)="openRequestsModal()" class="w-full text-left px-4 py-3 bg-neutral-950 hover:bg-neutral-800 rounded-xl font-bold transition-all text-sm flex items-center gap-3 border border-neutral-800/50">
              <span>📄</span> My Demand History
            </button>
          </div>
        </div>
      </div>

      <main class="max-w-xl mx-auto p-4 space-y-6">
        
        <!-- Tab Switcher -->
        <div class="flex bg-neutral-900 p-1 rounded-2xl relative">
          <div class="absolute top-1 bottom-1 w-[calc(50%-4px)] bg-neutral-800 rounded-xl transition-all duration-300"
               [class.left-1]="activeTab === 'favorites'"
               [class.left-[calc(50%+2px)]]="activeTab === 'all'">
          </div>
          <button (click)="activeTab = 'favorites'" 
                  class="flex-1 relative z-10 py-3 text-[10px] font-black uppercase tracking-widest transition-colors duration-300"
                  [class.text-yellow-500]="activeTab === 'favorites'"
                  [class.text-neutral-500]="activeTab !== 'favorites'">
            Favorites
          </button>
          <button (click)="activeTab = 'all'" 
                  class="flex-1 relative z-10 py-3 text-[10px] font-black uppercase tracking-widest transition-colors duration-300"
                  [class.text-yellow-500]="activeTab === 'all'"
                  [class.text-neutral-500]="activeTab !== 'all'">
            My Barbers
          </button>
        </div>

        <!-- Barber Cards -->
        <div class="space-y-4">
          <div *ngIf="displayBarbers.length === 0" class="text-center py-12 text-neutral-500 font-bold text-sm bg-neutral-900 rounded-[2.5rem] border border-neutral-800">
            No barbers found in this list. <br/>
            <button (click)="openSearchModal()" class="text-yellow-500 mt-2 underline">Search to add one</button>
          </div>

          <div *ngFor="let barber of displayBarbers" class="bg-neutral-900 border border-neutral-800 rounded-[2rem] overflow-hidden relative group">
            
            <!-- Context Menu (Remove / Fav) -->
            <div class="absolute top-4 right-4 flex items-center gap-2">
              <button (click)="toggleFavorite(barber.id)" class="p-2 rounded-full bg-neutral-950 border border-neutral-800 transition-all hover:bg-neutral-800">
                <span *ngIf="barber.favorite" class="text-yellow-500">★</span>
                <span *ngIf="!barber.favorite" class="text-neutral-600">☆</span>
              </button>
              <button (click)="removeBarber(barber.id)" class="p-2 rounded-full bg-red-900/20 border border-red-900/30 text-red-500 transition-all hover:bg-red-900/40">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
              </button>
            </div>

            <div class="p-6">
              <div class="flex items-center gap-4 mb-6">
                <!-- Avatar -->
                <div class="relative">
                  <div class="w-16 h-16 rounded-full bg-neutral-800 flex items-center justify-center border border-neutral-700 font-black text-xl text-neutral-400">
                    {{ barber.firstName.charAt(0) }}{{ barber.lastName.charAt(0) }}
                  </div>
                  <!-- Status Dot -->
                  <div class="absolute bottom-0 right-0 w-4 h-4 rounded-full border-2 border-neutral-900"
                       [ngClass]="getStatusColor(barber.currentStatus)"></div>
                </div>
                
                <div>
                  <h3 class="text-2xl font-black italic uppercase tracking-tighter">{{ barber.firstName }} {{ barber.lastName }}</h3>
                  <p class="text-[10px] font-black uppercase tracking-widest mt-1" [ngClass]="getStatusTextColor(barber.displayStatus)">
                    {{ barber.displayStatus }}
                  </p>
                </div>
              </div>

              <!-- Stats -->
<div class="grid grid-cols-2 gap-4 mb-6">
                
                <div class="bg-neutral-950 rounded-2xl p-4 border border-neutral-800 flex flex-col justify-center">
                  <p class="text-[10px] text-neutral-500 font-black uppercase tracking-widest mb-1">
                    Queue Position
                  </p>

                  <div *ngIf="!barber.inQueue">
                    <p class="font-black text-xl text-yellow-500">
                      {{ barber.queuePosition }}
                      <span class="text-[10px] text-neutral-500 ml-1 uppercase tracking-widest">waiting</span>
                    </p>
                  </div>

                  <div *ngIf="barber.inQueue">
                    
                    <!-- 1. Ila kan gals fel koursi kiy-7ssen (IN_PROGRESS) -->
                    <p *ngIf="getAcceptedApp()?.barberId === barber.id && getAcceptedApp()?.status === 'IN_PROGRESS'" class="font-black text-xl text-yellow-500 animate-pulse">
                      NOW
                      <span class="text-[10px] text-yellow-500/70 ml-1 uppercase tracking-widest"> In The Chair...</span>
                    </p>

                    <!-- 2. Ila kan yalah kiy-tsenna (WAITING) -->
                    <ng-container *ngIf="!(getAcceptedApp()?.barberId === barber.id && getAcceptedApp()?.status === 'IN_PROGRESS')">
                      
                      <!-- Hwa li taba3 (Position 0) -->
                      <p *ngIf="barber.queuePosition === 0" class="font-black text-xl text-green-500 animate-pulse">
                        NEXT
                        <span class="text-[10px] text-green-500/70 ml-1 uppercase tracking-widest">get ready</span>
                      </p>
                      
                      <!-- Baqi b3id (Position > 0) -->
                      <p *ngIf="barber.queuePosition > 0" class="font-black text-xl text-yellow-500">
                        {{ barber.queuePosition }}
                        <span class="text-[10px] text-neutral-500 ml-1 uppercase tracking-widest">ahead of you</span>
                      </p>

                    </ng-container>

                  </div>
                </div>

                <div class="bg-neutral-950 rounded-2xl p-4 border border-neutral-800 flex flex-col justify-center">
                  <p class="text-[10px] text-neutral-500 font-black uppercase tracking-widest mb-1">Est. Wait</p>
                  
                  <!-- حيدنا الـ span ودرنا الـ الميثود ديالنا -->
                  <p class="font-black text-xl text-yellow-500">
                    {{ formatWaitTime(barber.estimatedWaitTime) }}
                  </p>
                </div>

              </div>


              <button *ngIf="(barber.currentStatus === 'ACTIVE' || barber.currentStatus === 'ON_BREAK') && !isAlreadyRequested(barber.id) && !isAcceptedAnywhere()"
                      (click)="openServiceSelection(barber.id)"
                      class="w-full bg-yellow-500 text-black font-black uppercase py-4 rounded-xl hover:bg-yellow-400 mb-2 transition-all shadow-[0_0_15px_rgba(234,179,8,0.2)]">
                Send a Demand
              </button>

        <div *ngIf="isAlreadyRequested(barber.id)" 
            class="w-full bg-neutral-900 text-yellow-500/50 py-4 text-center rounded-xl border border-yellow-500/10 mb-2 uppercase font-black text-[10px] tracking-widest">
          Pending Response... 
<button (click)="cancelMyRequest(barber.id)" 
  class="mt-3 px-4 py-2 text-[10px] font-black uppercase tracking-widest
         text-red-400 rounded-lg
         bg-red-500/10 backdrop-blur-sm
         border border-red-500/20
         hover:bg-red-500/20 hover:text-red-300 hover:shadow-[0_0_12px_rgba(239,68,68,0.4)]
         active:scale-95
         transition-all duration-300">
  Cancel
</button>
        </div>


<div *ngIf="getAcceptedApp()?.barberId === barber.id && getAcceptedApp()?.status === 'WAITING'"
            class="w-full bg-neutral-950 text-green-500 text-center py-4 rounded-xl border border-green-500/20 shadow-inner mb-2">
          <span class="flex items-center justify-center gap-2 font-black uppercase text-[10px] tracking-widest">
            <span class="w-2 h-2 bg-green-500 rounded-full animate-pulse"></span>
            You are in his Queue
          </span>
        </div>

        <div *ngIf="getAcceptedApp()?.barberId === barber.id && getAcceptedApp()?.status === 'IN_PROGRESS'"
            class="w-full bg-yellow-500/10 text-yellow-500 text-center py-4 rounded-xl border border-yellow-500/30 shadow-[0_0_15px_rgba(234,179,8,0.2)] mb-2 transition-all">
          <span class="flex items-center justify-center gap-2 font-black uppercase text-[10px] tracking-widest">
            <span class="w-2 h-2 bg-yellow-500 rounded-full animate-ping"></span>
            ✂️ In The Chair...
          </span>
        </div>

        <div *ngIf="isAcceptedAnywhere() && getAcceptedApp()?.barberId !== barber.id"
            class="w-full bg-neutral-950 text-neutral-600 text-center py-4 rounded-xl border border-neutral-900 opacity-50 mb-2">
          <span class="text-[10px] font-black uppercase italic tracking-tighter">Busy with another barber</span>
        </div>

          <div *ngIf="(barber.currentStatus === 'OFFLINE' || barber.currentStatus === 'FULL') && !isAlreadyRequested(barber.id) && !isAcceptedAnywhere()"
              class="w-full bg-neutral-950 text-neutral-600 text-center font-black uppercase tracking-widest py-4 rounded-xl border border-neutral-900 shadow-inner cursor-not-allowed opacity-80">
            Not Available currently
          </div>
            </div>

            <!-- Accent line -->
            <div class="absolute bottom-0 inset-x-0 h-1.5" [ngClass]="getStatusColor(barber.currentStatus)"></div>
          </div>
        </div>

      </main>

      <!-- SEARCH MODAL -->
      <div *ngIf="searchModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" (click)="searchModalOpen = false"></div>
        <div class="relative w-full max-w-md bg-neutral-900 border border-neutral-800 rounded-[2rem] p-8 shadow-2xl flex flex-col max-h-[90vh]">
          <div class="flex justify-between items-center mb-6">
            <h2 class="text-2xl font-black italic uppercase tracking-tighter">Find Barber</h2>
            <button (click)="searchModalOpen = false" class="text-neutral-500 hover:text-white">✕</button>
          </div>

          <input type="text" [(ngModel)]="searchQuery" (input)="onSearch()" 
                 class="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-4 py-3 text-sm font-bold text-white mb-4 focus:outline-none focus:border-yellow-500" 
                 placeholder="Search by name or phone...">
          
          <div class="overflow-y-auto flex-1 pr-2 -mr-2 space-y-3">
            <div *ngIf="searchResults.length === 0 && searchQuery.length > 2" class="text-center p-4 text-neutral-500 font-bold text-xs mt-4">
              No barbers found.
            </div>
            
            <div *ngFor="let res of searchResults" class="bg-neutral-950 border border-neutral-800 rounded-xl p-4 flex justify-between items-center">
              <div>
                <p class="font-black text-sm">{{ res.firstName }} {{ res.lastName }}</p>
                <p class="text-[10px] uppercase font-bold text-neutral-500 mt-0.5">{{ res.currentStatus }}</p>
              </div>
              <button (click)="addBarber(res.id)" class="bg-yellow-500 text-black font-black uppercase tracking-widest text-[10px] px-4 py-2 rounded-lg hover:bg-yellow-400">
                Add
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- SERVICE SELECTION MODAL -->
      <div *ngIf="serviceSelectOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" (click)="serviceSelectOpen = false"></div>
        <div class="relative w-full max-w-md bg-neutral-900 border border-neutral-800 rounded-[2rem] p-8 shadow-2xl flex flex-col max-h-[90vh]">
          
          <h2 class="text-2xl font-black italic uppercase tracking-tighter mb-2">Request Appt.</h2>
          <p class="text-xs font-bold text-neutral-400 mb-6">Select services you want for this appointment.</p>
          
          <div class="overflow-y-auto flex-1 pr-2 -mr-2 space-y-3">
            <div *ngIf="barberServices.length === 0" class="text-center p-4 text-neutral-500 font-bold text-xs mt-4">
              Loading services or barber has no services set.
            </div>

            <!-- 🔥 L-FIX 1: Bdelna barberServices b displayServices -->
            <label *ngFor="let srv of displayServices" 
                   class="flex items-center gap-3 bg-neutral-950 border border-neutral-800 p-4 rounded-2xl cursor-pointer hover:border-neutral-600 transition-all"
                   [ngClass]="{'border-yellow-500 bg-yellow-500/5': selectedServices.includes(srv.id)}">
              <input type="checkbox" [value]="srv.id" (change)="toggleService(srv.id)" class="accent-yellow-500 w-4 h-4">
              <div class="flex-1">
                <p class="font-bold text-sm">{{ srv.name }}</p>
                <div class="flex items-center gap-2 mt-1">
                  <span class="text-[10px] font-black uppercase text-neutral-500">{{ srv.duration }}</span>
                  
                  <!-- 🔥 L-FIX 2: Zidna l-blaka dyal CUSTOM hna -->
                  <span *ngIf="srv.isCustom" class="text-[8px] font-black uppercase bg-green-500/20 text-green-500 px-1.5 py-0.5 rounded">CUSTOM</span>
                  
                </div>
              </div>
            </label>
          </div>

          <div class="mt-8 flex gap-3 pt-4 border-t border-neutral-800">
            <button (click)="closeModal()" class="flex-1 bg-neutral-950 border border-neutral-800 text-neutral-400 font-black uppercase tracking-widest py-3 rounded-xl hover:text-white">Cancel</button>
            <button (click)="submitRequest()" 
                    [disabled]="selectedServices.length === 0 || isSubmitting"
                    class="flex-1 bg-yellow-500 text-black font-black uppercase tracking-widest py-3 rounded-xl transition-all
                           hover:bg-yellow-400 disabled:opacity-50 disabled:cursor-not-allowed">
              {{ isSubmitting ? 'SENDING...' : 'CONFIRM' }}
            </button>
          </div>
        </div>
      </div>
    <!-- 🔥 MY REQUESTS MODAL 🔥 -->
      <div *ngIf="isRequestsModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" (click)="closeRequestsModal()"></div>
        <div class="relative w-full max-w-md bg-neutral-900 border border-neutral-800 rounded-[2rem] p-6 shadow-2xl flex flex-col max-h-[90vh]">
          
          <div class="flex justify-between items-center mb-6">
            <h2 class="text-2xl font-black italic uppercase tracking-tighter">My Requests</h2>
            <button (click)="closeRequestsModal()" class="p-2 text-neutral-500 hover:text-white rounded-full bg-neutral-950 border border-neutral-800 transition-colors">✕</button>
          </div>

          <!-- TABS (Tailwind Style) -->
          <div class="flex bg-neutral-950 p-1 rounded-xl mb-4 border border-neutral-800 relative">
            <button (click)="setActiveRequestsTab('ACCEPTED')" 
                    class="flex-1 py-2.5 text-[10px] font-black uppercase tracking-widest rounded-lg transition-all duration-300"
                    [ngClass]="activeRequestsTab === 'ACCEPTED' ? 'bg-neutral-800 text-white shadow-sm' : 'text-neutral-500 hover:text-neutral-400'">
              Accepted
            </button>
            <button (click)="setActiveRequestsTab('PENDING')" 
                    class="flex-1 py-2.5 text-[10px] font-black uppercase tracking-widest rounded-lg transition-all duration-300"
                    [ngClass]="activeRequestsTab === 'PENDING' ? 'bg-neutral-800 text-white shadow-sm' : 'text-neutral-500 hover:text-neutral-400'">
              Pending
            </button>
            <button (click)="setActiveRequestsTab('REJECTED')" 
                    class="flex-1 py-2.5 text-[10px] font-black uppercase tracking-widest rounded-lg transition-all duration-300"
                    [ngClass]="activeRequestsTab === 'REJECTED' ? 'bg-neutral-800 text-white shadow-sm' : 'text-neutral-500 hover:text-neutral-400'">
              Rejected
            </button>
          </div>

          <!-- LISTA DYAL REQUESTS -->
          <div class="overflow-y-auto flex-1 pr-2 -mr-2 space-y-3">
            <div *ngIf="filteredRequests.length === 0" class="text-center p-6 text-neutral-500 font-bold text-xs mt-4 bg-neutral-950/50 rounded-2xl border border-neutral-800/50 dashed">
              No requests found in this section.
            </div>
            
            <div *ngFor="let req of filteredRequests" class="bg-neutral-950 border border-neutral-800 rounded-2xl p-4 flex justify-between items-center group hover:border-neutral-700 transition-colors">
              <div>
                <h4 class="font-black text-sm uppercase text-white">{{ req.firstName || 'Barber' }}</h4>
                <p class="text-[10px] font-black text-neutral-500 uppercase tracking-widest mt-1">
                  <!-- Ila 3ndu list d services, afichihom, sinon dir default text -->
                  <ng-container *ngIf="req.serviceNames && req.serviceNames.length > 0; else noSrv">
                    {{ req.serviceNames.join(' + ') }}
                  </ng-container>
                  <ng-template #noSrv>BARBER REQUEST</ng-template>
                </p>
              </div>
              
              <!-- THE BADGE -->
              <span class="px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-widest"
                    [ngClass]="{
                      'bg-green-500/10 text-green-500 border border-green-500/20': activeRequestsTab === 'ACCEPTED',
                      'bg-yellow-500/10 text-yellow-500 border border-yellow-500/20': activeRequestsTab === 'PENDING',
                      'bg-red-500/10 text-red-500 border border-red-500/20': activeRequestsTab === 'REJECTED'
                    }">
                {{ getBadgeText(req.status) }}
              </span>
            </div>
          </div>
          
        </div>
      </div>
    </div>
  `
})
export class ClientDashboard implements OnInit, OnDestroy {
  currentUser: User | null = null;
  activeTab: 'favorites' | 'all' = 'favorites';
  
  myBarbers: BarberSearchDto[] = [];
  myFavorites: BarberSearchDto[] = [];
  
  drawerOpen = false;
  
  searchModalOpen = false;
  searchQuery = '';
  isRequestsModalOpen = false;
  activeRequestsTab: 'ACCEPTED' | 'PENDING' | 'REJECTED' = 'ACCEPTED';
  myRequests: AppointmentResponseDTO[] = [];
  searchResults: BarberSearchDto[] = [];

  serviceSelectOpen = false;
  targetBarberId: number | null = null;
  barberServices: ServiceResponseDTO[] = [];
  displayServices: any[] = [];
  selectedServices: number[] = [];
  userAppointments: AppointmentResponseDTO[] = [];
  isSubmitting = false;
  // private subscriptions: Subscription[] = [];
  private barberSubs: Subscription[] = [];
  private userSubs: Subscription[] = [];
  private waitTimer: any;
  constructor(
    private auth: AuthService,  
    private router: Router,
    private barberService: BarberService,
    private ws: WebsocketService,
    private appointmentService: AppointmentService,
    private catalogService: ServiceCatalogService,
    private cdr: ChangeDetectorRef,
    private zone: NgZone,
  ) {}

  ngOnInit() {
    this.currentUser = this.auth.getCurrentUser();
    console.log(this.currentUser);
    
    this.ws.connect();
    this.loadLists();
    // 1. Jib status mlli y-7ell l-app (ila kan dejà m-zid)
    this.loadMyStatus();
    
    // 2. Tsennay l-jdid real-time

       this.initUserWebSocket();
       this.startWaitTimer();

  }
  // 🔥 MÉTHODES DYAL MY REQUESTS 🔥
  loadMyRequests() {
    // ⚠️ T2ekked blli zedti getMyRequests() f AppointmentService kima chre7t lik qbel
    this.appointmentService.getMyRequests().subscribe({
      next: (data) => {
        this.myRequests = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur f tahmil requests', err)
    });
  }

  openRequestsModal() {
    this.drawerOpen = false; // Sed l-menu l-kbir
    this.isRequestsModalOpen = true; // 7el l-modal d requests
    this.loadMyRequests(); // Jib data mn l-backend
  }

  closeRequestsModal() {
    this.isRequestsModalOpen = false;
    this.cdr.detectChanges();
  }

  setActiveRequestsTab(tab: 'ACCEPTED' | 'PENDING' | 'REJECTED') {
    this.activeRequestsTab = tab;
  }

  get filteredRequests() {
    return this.myRequests.filter(req => {
      if (this.activeRequestsTab === 'ACCEPTED') {
        return ['WAITING', 'IN_PROGRESS', 'COMPLETED'].includes(req.status);
      } else if (this.activeRequestsTab === 'PENDING') {
        return req.status === 'PENDING';
      } else if (this.activeRequestsTab === 'REJECTED') {
        return req.status === 'CANCELLED';
      }
      return false;
    });
  }

  getBadgeText(status: string): string {
    switch (status) {
      case 'IN_PROGRESS': return 'IN CHAIR';
      case 'WAITING': return 'ACCEPTED';
      case 'COMPLETED': return 'DONE';
      case 'PENDING': return 'PENDING';
      case 'CANCELLED': return 'REJECTED';
      default: return status;
    }
  }
  initUserWebSocket() {
    if (this.currentUser && this.currentUser.id) {
      const sub = this.ws.subscribeToUser(this.currentUser.id).subscribe(msg => {
        console.log("WebSocket Message received for User:", msg);
        
        if (msg === 'QUEUE_UPDATED') {
          // 1. N-tsennaw 1.5 sanya t-tsajjel f DB
          setTimeout(() => {
            // 2. 🔥 Hna 3ad n-fiyyqou Angular bach y-rsem chacha
            this.zone.run(() => {
              this.loadMyStatus();
            });
          }, 500); 
        }
      });
      this.userSubs.push(sub);
    }
  }
  isAlreadyRequested(barberId: number): boolean {
    // Checki wach kayn chi appointment f l-array 3ndu had l-barberId u status PENDING
    return this.userAppointments.some(a => a.barberId === barberId && a.status === 'PENDING');
  }
  // 1. Had l-function katti-9elleb f l-lista wach chi wa7ed m-accepter
  getAcceptedApp(): AppointmentResponseDTO | undefined {
    return this.userAppointments.find(a => 
      a.status === 'WAITING' || a.status === 'IN_PROGRESS'
    );
  }

  // 2. Check wach m-accepter f chi blassa (bach n-bloquiweh f l-okhrin)
  isAcceptedAnywhere(): boolean {
    return !!this.getAcceptedApp();
  }
  loadMyStatus() {
    this.appointmentService.getMyActiveAppointment().subscribe(res => {
      if (this.serviceSelectOpen) return;
      console.log("Data li jat mn DB ba3d l-message:", res);
      // Daba res wellat Array []
      this.userAppointments = res; 
      
      this.cdr.detectChanges();
    });
  }

  ngOnDestroy() {
    this.barberSubs.forEach(s => s.unsubscribe());
    this.userSubs.forEach(s => s.unsubscribe());
    this.ws.disconnect();
    // 🔥 ZID HADI HNA: Tfi l-Magana
    if (this.waitTimer) {
      clearInterval(this.waitTimer);
    }
  }

  logout() {
    localStorage.removeItem('token');
    this.router.navigate(['/']);
  }

  get displayBarbers() {
    return this.activeTab === 'favorites' ? this.myFavorites : this.myBarbers;
  }

  loadLists() {
    this.barberSubs.forEach(s => s.unsubscribe());
    this.barberSubs = [];

    const now = new Date().getTime(); // 👈 L-waqt d daba

    this.barberService.getMyBarbers().subscribe(list => {
      // 🔥 L-JDID: N-sajjlou targetTime (Waqt n-nihaya l-moudbot)
      this.myBarbers = list.map(b => ({
        ...b,
        targetTime: b.estimatedWaitTime ? now + (b.estimatedWaitTime * 60000) : null
      }));
      this.subscribeToBarbers(this.myBarbers);
    });

    this.barberService.getMyFavorites().subscribe(list => {
      this.myFavorites = list.map(b => ({
        ...b,
        targetTime: b.estimatedWaitTime ? now + (b.estimatedWaitTime * 60000) : null
      }));
      this.subscribeToBarbers(this.myFavorites);
    });
  }

  // subscribeToBarbers(list: BarberSearchDto[]) {
  //   this.barberSubs.forEach(s => s.unsubscribe());
  //   this.barberSubs = [];

  //   list.forEach(b => {
  //     const sub = this.ws.subscribeToBarberStatus(b.id).subscribe(newStatus => {
  //       const update = (arr: BarberSearchDto[]) => {
  //         const found = arr.find(x => x.id === b.id);
  //         if (found) found.currentStatus = newStatus as any;
  //       };

  //       update(this.myBarbers);
  //       update(this.myFavorites);

  //       this.cdr.detectChanges();
  //     });

  //     this.barberSubs.push(sub);
  //   });
  // }
  // 1. L-Méthode li katti-tsennet l-WebSocket
  subscribeToBarbers(list: BarberSearchDto[]) {
    // Msa7 l-wden l-qdam bach may-t-dbblouch
    this.barberSubs.forEach(s => s.unsubscribe());
    this.barberSubs = [];

    list.forEach(b => {
      // 👈 Hna katti-tsennet l-ay bedil wqe3 3nd l-coiffeur
      // (Ila knti msmiha f ws service "subscribeToBarberStatus", khdem biha)
      const sub = this.ws.subscribeToQueue(b.id).subscribe(msg => {
        console.log("Signal wsel men 3nd l-barber " + b.id + " :", msg);
        
        // 🔥 Hada hwa s-ser: melli y-wsel signal, tsenna chwiya w 3iyyet l-API t-jib l-jdid
          setTimeout(() => {
            // 🔥 Fiyyeq Angular!
            this.zone.run(() => {
              this.refreshBarberData();
            });
          }, 500);
      });

      this.barberSubs.push(sub);
    });
  }

  // 2. L-Méthode li katti-jib l-Data jdida mn l-Backend bla ma t-rebel l-App
  refreshBarberData() {
    const now = new Date().getTime();

    this.barberService.getMyBarbers().subscribe(list => {
      this.myBarbers = list.map(b => ({
        ...b,
        targetTime: b.estimatedWaitTime ? now + (b.estimatedWaitTime * 60000) : null
      }));
      this.cdr.detectChanges();
    });

    this.barberService.getMyFavorites().subscribe(list => {
      this.myFavorites = list.map(b => ({
        ...b,
        targetTime: b.estimatedWaitTime ? now + (b.estimatedWaitTime * 60000) : null
      }));
      this.cdr.detectChanges();
    });
    this.loadMyStatus();
  }
  
// 🔥 L-Magana m-gadda: Katti-Freeze l-waqt ila l-coiffeur baqi ma-wrekch 3la START
  startWaitTimer() {
    this.waitTimer = setInterval(() => {
      const now = new Date().getTime();

      const updateBarberTime = (b: any) => {
        // 1. N-checkiw wach l-coiffeur m-beddi l-khdma (Khddam f chi wahed daba)
        // ⚠️ HNA: T2ekked mn s-smiya d status li katti-wlli 3ndu melli kiy-wrek 3la START
        const isWorking = b.displayStatus === 'Working';

        if (b.targetTime) {
          if (isWorking) {
            // ✅ Ila khddam (Wrek 3la START) -> nqess l-waqt 3adi
            if (b.targetTime > now) {
              b.estimatedWaitTime = Math.ceil((b.targetTime - now) / 60000);
            } else {
              b.estimatedWaitTime = 0;
            }
          }else {
            // 🛑 Ila jales (Ma-wrekch 3la START) -> FREEZE L-WAQT!
            // Kan-zidou 10 twani (10000 ms) f targetTime bach l-fareq y-bqa howa howa u ma-y-n9ess-ch f HTML
            b.targetTime = now + (b.estimatedWaitTime * 60000);
          } 
        }
      };

      // Tbiq l-qaleb 3la ga3 l-listes
      this.myBarbers.forEach(updateBarberTime);
      this.myFavorites.forEach(updateBarberTime);

      this.cdr.detectChanges();
      
    }, 10000); // katti-t-executa kola 10 twani
  }

  getStatusColor(status: string) {
    if (status === 'ACTIVE') return 'bg-green-500';
    if (status === 'ON_BREAK') return 'bg-yellow-500';
    if (status === 'FULL') return 'bg-orange-500';
    return 'bg-neutral-600';
  }

  // getStatusTextColor(status: string) {
  //   if (status === 'ACTIVE') return 'text-green-500';
  //   if (status === 'FULL') return 'text-orange-500';
  //   return 'text-neutral-500';
  // }
  getStatusTextColor(status: string) {
    if (status === 'OPEN' || status === 'Working') return 'text-green-500';
    if (status === 'ON_BREAK') return 'text-yellow-500';  
    return 'text-neutral-500'; // CLOSED
  }

  toggleFavorite(barberId: number) {
    this.barberService.toggleFavorite(barberId).subscribe(() => {
      this.loadLists();
    });
  }

  removeBarber(barberId: number) {
    this.barberService.removeBarber(barberId).subscribe(() => {
      this.loadLists();
    });
  }

  // --- Search functionality ---
  openSearchModal() {
    this.searchModalOpen = true;
    this.drawerOpen = false;
    this.searchQuery = '';
    this.searchResults = [];
  }

  onSearch() {
    if (this.searchQuery.length > 2) {
      this.barberService.searchBarbers(this.searchQuery).subscribe(res => {
        this.searchResults = res;
      });
    } else {
      this.searchResults = [];
    }
  }

  addBarber(id: number) {
    this.barberService.addBarber(id).subscribe(() => {
      this.searchModalOpen = false;
      this.loadLists();
      this.activeTab = 'all'; // switch to all tab to show it
    });
  }

  // --- Appointment Request ---
openServiceSelection(barberId: number) {
    this.targetBarberId = barberId;
    this.selectedServices = [];
    this.barberServices = [];
    this.displayServices = []; // n-khewiwha f l-lowel

    // 1. Jib l-khedmat l-3adiyin dyal l-barber
    this.catalogService.getBarberServices(barberId).subscribe(res => {
      this.barberServices = res;
      
      // 2. Jib l-awqat l-mkhasssa dyal had l-klyan
      this.barberService.getMyCustomTimesForBarber(barberId).subscribe({
        next: (customData: any[]) => {
          // 3. Khlet l-3adi m3a l-mkhasses
          this.displayServices = this.barberServices.map(defaultSrv => {
            const custom = customData.find(c => c.serviceId === defaultSrv.id);
            if (custom && custom.customDuration) {
              return { 
                ...defaultSrv, 
                duration: custom.customDuration + ' MIN', // Zidna MIN bach t-bqa m-qadda
                isCustom: true 
              };
            }
            return { ...defaultSrv, isCustom: false };
          });
          
          this.serviceSelectOpen = true;   
          this.cdr.detectChanges();
        },
        error: () => {
          // Ila wqe3 mochkil wla makanch waqt mkhasses, khelli l-3adi
          this.displayServices = this.barberServices.map(s => ({...s, isCustom: false}));
          this.serviceSelectOpen = true;   
          this.cdr.detectChanges();
        }
      });
    });
  }

  toggleService(id: number) {
    const idx = this.selectedServices.indexOf(id);
    if (idx > -1) {
      this.selectedServices.splice(idx, 1);
    } else {
      this.selectedServices.push(id);
    }
  }
  closeModal() {
    this.serviceSelectOpen = false;
    this.cdr.detectChanges();
  }

  submitRequest() {
    if (this.selectedServices.length === 0 || !this.targetBarberId || this.isSubmitting) return;
    this.isSubmitting = true;
    const payload: AppointmentRequestDTO = {
      barberId: this.targetBarberId,
      serviceIds: this.selectedServices,
    };

    this.appointmentService.createAppointment(payload).subscribe({
      next: (res) => {
        console.log(payload);
        
              // 🔥 زيدها مباشرة فـ state
      this.userAppointments.push({
        ...res,
        status: 'PENDING'
      });
        console.log(payload);
        
        this.serviceSelectOpen = false;
          this.isSubmitting = false;
         this.cdr.detectChanges();
        //this.loadLists();
      },
      error: (err) => {
        this.isSubmitting = false; // 🔥 Rje3ha false 7ta ila wqe3 mouchkil

        // N-chedou l-Error mn l-Backend
        if (err.error === 'ALREADY_PENDING' || err.error?.message === 'ALREADY_PENDING' || err.message?.includes('ALREADY_PENDING')) {
           alert("⚠️ Dejà msifet demande l had l-coiffeur!");
           this.serviceSelectOpen = false;
        } else {
           alert("⚠️ Wqe3 mochkil, 3awd jerreb.");
        }
      }
    });
  }
  cancelMyRequest(barberId: number) {
    // 1. Jbed l-appointment PENDING li m3a had l-barber men l-lista dyalna
    const app = this.userAppointments.find(a => a.barberId === barberId && a.status === 'PENDING');
    
    if (app) {
      // 2. Nadi l-service (ista3mel l-methode li 3ndek dejà f l-service)
      this.appointmentService.rejectAppointment(app.id).subscribe({
        next: () => {
          // Refresh bach t-t-7iyed l-box d Pending
          this.loadMyStatus();
        },
        error: (err) => console.error("Error canceling", err)
      });
    }
  }
  formatWaitTime(minutes: number | null): string {
    if (minutes === null || minutes === undefined) return "0min";
    if (minutes < 60) return minutes + "min";
    
    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;
    
    if (remainingMinutes === 0) {
      return hours + "h";
    }
    return hours + "h " + remainingMinutes + "min";
  }
}
