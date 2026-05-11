import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  template: `
    <div class="min-h-screen bg-neutral-950 flex flex-col items-center justify-center p-4 relative overflow-hidden">
      <!-- Decorative background blur -->
      <div class="absolute top-1/4 left-1/2 -translate-x-1/2 w-96 h-96 bg-yellow-500/10 rounded-full blur-[120px] pointer-events-none"></div>

      <div class="w-full max-w-md bg-neutral-900 border border-neutral-800 rounded-[2.5rem] p-8 z-10 shadow-2xl relative">
        
        <!-- Logo -->
        <div class="flex justify-center mb-8">
          <div class="w-14 h-14 bg-yellow-500 rounded-2xl flex items-center justify-center shadow-[0_0_20px_rgba(234,179,8,0.2)] rotate-3">
            <span class="text-neutral-950 font-black text-xl tracking-tighter">T7L</span>
          </div>
        </div>

        <!-- Role Toggle -->
        <div class="flex bg-neutral-950 p-1 rounded-2xl mb-8 relative">
          <!-- Active Highlight -->
          <div 
            class="absolute top-1 bottom-1 w-[calc(50%-4px)] bg-neutral-800 rounded-xl transition-all duration-300"
            [class.left-1]="role === 'CLIENT'"
            [class.left-[calc(50%+2px)]]="role === 'COIFFEUR'">
          </div>
          
          <button 
            type="button"
            (click)="setRole('CLIENT')"
            class="flex-1 relative z-10 py-3 text-xs font-black uppercase tracking-widest transition-colors duration-300"
            [class.text-yellow-500]="role === 'CLIENT'"
            [class.text-neutral-500]="role !== 'CLIENT'">
            Client
          </button>
          <button 
            type="button"
            (click)="setRole('COIFFEUR')"
            class="flex-1 relative z-10 py-3 text-xs font-black uppercase tracking-widest transition-colors duration-300"
            [class.text-yellow-500]="role === 'COIFFEUR'"
            [class.text-neutral-500]="role !== 'COIFFEUR'">
            Barber
          </button>
        </div>

        <h2 class="text-2xl font-black text-white italic uppercase tracking-tight text-center mb-6">
          {{ isLogin ? 'Welcome Back' : 'Create Account' }}
        </h2>

        <!-- Error Alert -->
        <div *ngIf="errorMessage" class="mb-6 p-4 bg-red-900/20 border border-red-900/30 rounded-xl">
          <p class="text-red-500 text-xs font-bold">{{ errorMessage }}</p>
        </div>
        
        <div *ngIf="successMessage" class="mb-6 p-4 bg-green-900/20 border border-green-900/30 rounded-xl">
          <p class="text-green-500 text-xs font-bold">{{ successMessage }}</p>
        </div>

        <!-- Form -->
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="space-y-4">
          
          <!-- Register Fields -->
          <ng-container *ngIf="!isLogin">
            <div class="flex gap-4">
              <div class="flex-1 space-y-1">
                <label class="text-[10px] font-black uppercase tracking-widest text-neutral-500 ml-1">First Name</label>
                <input formControlName="firstName" type="text" 
                  class="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-4 py-3 text-sm font-bold text-white focus:outline-none focus:border-yellow-500 focus:ring-1 focus:ring-yellow-500 transition-colors placeholder:text-neutral-700" 
                  placeholder="John">
              </div>
              <div class="flex-1 space-y-1">
                <label class="text-[10px] font-black uppercase tracking-widest text-neutral-500 ml-1">Last Name</label>
                <input formControlName="lastName" type="text" 
                  class="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-4 py-3 text-sm font-bold text-white focus:outline-none focus:border-yellow-500 focus:ring-1 focus:ring-yellow-500 transition-colors placeholder:text-neutral-700" 
                  placeholder="Doe">
              </div>
            </div>

            
            </ng-container>
            
            <!-- Common Fields -->
            <div class="space-y-1">
              <label class="text-[10px] font-black uppercase tracking-widest text-neutral-500 ml-1">Phone Number</label>
              
              <input 
                formControlName="phoneNumber" 
                type="tel" 
                class="w-full bg-neutral-950 border rounded-xl px-4 py-3 text-sm font-bold text-white focus:outline-none transition-all"
                [class.border-red-500]="isFieldInvalid('phoneNumber')"
                [class.border-neutral-800]="!isFieldInvalid('phoneNumber')"
                [class.focus:border-yellow-500]="!isFieldInvalid('phoneNumber')"
                placeholder="0612345678">

              <div *ngIf="isFieldInvalid('phoneNumber')" class="mt-1 ml-1 flex flex-col gap-1">
                <span *ngIf="form.get('phoneNumber')?.errors?.['required']" class="text-red-500 text-[10px] font-bold italic">
                  Had l-7aqal darouri!
                </span>
                <span *ngIf="form.get('phoneNumber')?.errors?.['pattern']" class="text-red-500 text-[10px] font-bold italic">
                  Had l-u-katba "jvwdjjd" machi nemra. Khas 10 d l-arqaam!
                </span>
              </div>
            </div>

          <div class="space-y-1">
            <label class="text-[10px] font-black uppercase tracking-widest text-neutral-500 ml-1">Password</label>
              <div class="relative">
                <input 
                  formControlName="password" 
                  [type]="showPassword ? 'text' : 'password'" 
                  class="w-full bg-neutral-950 border rounded-xl px-4 py-3 pr-12 text-sm font-bold text-white focus:outline-none transition-all"
                  [class.border-red-500]="isFieldInvalid('password')"
                  [class.border-neutral-800]="!isFieldInvalid('password')"
                  placeholder="••••••••">
              
              <!-- L-icon ban ghi ila kant l-katba (value length > 0) -->
              <button 
                *ngIf="form.get('password')?.value?.length > 0"
                type="button"
                (click)="showPassword = !showPassword"
                class="absolute right-4 top-1/2 -translate-y-1/2 text-neutral-500 hover:text-yellow-500 transition-colors">
                
                <!-- Eye-Off (Slash) -> t-ban mlli y-koun l-password m-khabbi (showPassword = false) -->
                <svg *ngIf="!showPassword" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 0 0 1.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.451 10.451 0 0 1 12 4.5c4.756 0 8.773 3.162 10.065 7.498a10.522 10.522 0 0 1-4.293 5.774M6.228 6.228 3 3m3.228 3.228 3.65 3.65m7.894 7.894L21 21m-3.228-3.228-3.65-3.65m0 0a3 3 0 1 0-4.243-4.243m4.242 4.242L9.88 9.88" />
                </svg>

                <!-- Eye (Open) -> t-ban mlli y-koun l-password bayen (showPassword = true) -->
                <svg *ngIf="showPassword" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 0 1 0-.644C3.483 8.653 7.377 6 12 6s8.517 2.653 9.964 5.678c.045.129.045.258 0 .387-1.447 3.025-5.341 5.678-12 5.678s-8.517-2.653-9.964-5.678Z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
                </svg>

              </button>
            </div>

              <div *ngIf="isFieldInvalid('password')" class="mt-1 ml-1 flex flex-col gap-1">
                  <span *ngIf="form.get('password')?.errors?.['required']" class="text-red-500 text-[10px] font-bold italic">
                    Password darouri!
                  </span>
                  <!-- Had l-message gha-yban ila l-user madarch Majuscule walla Minuscule walla Raqm -->
                  <span *ngIf="form.get('password')?.errors?.['pattern']" class="text-red-500 text-[10px] font-bold italic">
                    Khass t-dir 7arf kbir, 7arf sghir u raqm (u 6 d l-7ourouf l-aqall).
                  </span>
                </div>
          </div>

          <div class="space-y-1" *ngIf="!isLogin">
            <label class="text-[10px] font-black uppercase tracking-widest text-neutral-500 ml-1">Confirm Password</label>
              <div class="relative">
                <input 
                  formControlName="confirmPassword" 
                  [type]="showConfirmPassword ? 'text' : 'password'" 
                  class="w-full bg-neutral-950 border rounded-xl px-4 py-3 pr-12 text-sm font-bold text-white focus:outline-none transition-all"
                  [class.border-red-500]="form.hasError('mismatch') && form.get('confirmPassword')?.touched"
                  [class.border-neutral-800]="!(form.hasError('mismatch') && form.get('confirmPassword')?.touched)"
                  placeholder="••••••••">
              
              <!-- Button Show/Hide for Confirm Password -->
              <button 
                *ngIf="form.get('confirmPassword')?.value?.length > 0"
                type="button"
                (click)="showConfirmPassword = !showConfirmPassword"
                class="absolute right-4 top-1/2 -translate-y-1/2 text-neutral-500 hover:text-yellow-500 transition-colors">
                
                <!-- Eye-Off (m-khabbi) -->
                <svg *ngIf="!showConfirmPassword" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 0 0 1.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.451 10.451 0 0 1 12 4.5c4.756 0 8.773 3.162 10.065 7.498a10.522 10.522 0 0 1-4.293 5.774M6.228 6.228 3 3m3.228 3.228 3.65 3.65m7.894 7.894L21 21m-3.228-3.228-3.65-3.65m0 0a3 3 0 1 0-4.243-4.243m4.242 4.242L9.88 9.88" />
                </svg>

                <!-- Eye (bayen) -->
                <svg *ngIf="showConfirmPassword" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 0 1 0-.644C3.483 8.653 7.377 6 12 6s8.517 2.653 9.964 5.678c.045.129.045.258 0 .387-1.447 3.025-5.341 5.678-12 5.678s-8.517-2.653-9.964-5.678Z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
                </svg>
              </button>
            </div>
                
              <p *ngIf="form.hasError('mismatch') && form.get('confirmPassword')?.touched" 
                class="text-red-500 text-[10px] mt-1 ml-1 font-bold italic">
                L-passwords machi b7al b7al!
              </p>
          </div>

          <button 
            type="submit"
            [disabled]="loading || (isLogin ? (form.get('phoneNumber')?.invalid || form.get('password')?.invalid) : form.invalid)"
            class="w-full bg-yellow-500 text-black font-black uppercase tracking-widest py-4 rounded-xl hover:bg-yellow-400 transition-all active:scale-[0.98] mt-6 disabled:opacity-50 disabled:cursor-not-allowed flex justify-center">
            <span *ngIf="!loading">{{ isLogin ? 'Sign In' : 'Sign Up' }}</span>
            <span *ngIf="loading" class="animate-pulse">Loading...</span>
          </button>

        </form>

        <!-- Toggle Mode -->
        <div class="mt-8 text-center">
          <p class="text-neutral-500 text-xs font-bold">
            {{ isLogin ? "Don't have an account?" : "Already have an account?" }}
            <button 
              type="button" 
              (click)="toggleMode()"
              class="text-yellow-500 ml-1 hover:text-yellow-400 uppercase tracking-widest">
              {{ isLogin ? 'Register' : 'Login' }}
            </button>
          </p>
        </div>

      </div>
    </div>
  `
})
export class Auth implements OnInit {
  form: FormGroup;
  isLogin = true;
  role: 'CLIENT' | 'COIFFEUR' = 'CLIENT';
  loading = false;
  errorMessage = '';
  successMessage = '';
  showPassword = false;
  showConfirmPassword = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.form = this.fb.group({
      password: ['', {
        validators: [Validators.required, Validators.pattern(/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d).{6,}$/)],
        updateOn: 'change'
      }],
      firstName: ['', { validators: [], updateOn: 'blur' }],
      lastName: ['', { validators: [], updateOn: 'blur' }],
      phoneNumber: ['', {
        validators: [Validators.required, Validators.pattern('^[0-9]{10}$')],
        updateOn: 'blur'
      }],
      // حيد Validators.required من هنا فالبدية
      confirmPassword: [''] 
    }, { 
      validators: this.passwordMatchValidator.bind(this) 
    });
  }

  ngOnInit() {
    // this.route.queryParams.subscribe(params => {
    //   if (params['role'] === 'COIFFEUR' || params['role'] === 'CLIENT') {
    //     this.role = params['role'];
    //   }
    // });
    
    if (this.authService.isLoggedIn()) {
      this.redirectByRole(this.authService.getUserRole());
    }
    this.isLogin = true; 
    this.toggleMode();   
    this.toggleMode();
  }

  isFieldInvalid(field: string): boolean {
    const control = this.form.get(field);
    return !!(control && control.invalid && (control.touched || control.dirty));
  }

  setRole(r: 'CLIENT' | 'COIFFEUR') {
    this.role = r;
  }

  toggleMode() {
    this.isLogin = !this.isLogin;
    this.errorMessage = '';
    this.successMessage = '';

    const registerFields = ['firstName', 'lastName', 'confirmPassword'];

    registerFields.forEach(fieldName => {
      const control = this.form.get(fieldName);
      if (control) {
        if (!this.isLogin) {
          // فاش نكونو في REGISTER: ردهم ضروريين
          control.setValidators([Validators.required]);
        } else {
          // فاش نكونو في LOGIN: حيد كولشي
          control.clearValidators();
          control.setErrors(null); 
        }
        control.updateValueAndValidity();
      }
    });

    // تنظيف الـ Form Group كلو من أي mismatch قديم
    this.form.setErrors(null);
    this.form.updateValueAndValidity();
  }

  onSubmit() {

    if (this.form.invalid) {
      // Hadi kat-khalli ayy input fih error y-welli 7mer 
      this.form.markAllAsTouched();
      this.errorMessage = 'Please fix the errors in the form.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const val = this.form.value;

    if (this.isLogin) {
      this.authService.login({ phoneNumber: val.phoneNumber, password: val.password }).subscribe({
        next: (res: any) => {
          console.log(res);
          
          localStorage.setItem('token', res.token);
          this.redirectByRole(res.role);
        },
        error: (err) => {
          this.loading = false;
          this.errorMessage = 'Numéro de téléphone ou mot de passe incorrect';
        }
      });
    } else {

      this.authService.register({
        firstName: val.firstName,
        lastName: val.lastName,
        phoneNumber: val.phoneNumber,
        password: val.password,
        confirmPassword: val.confirmPassword,
        role: this.role
      }).subscribe({
        next: (res) => {
          this.loading = false;
          this.successMessage = 'Registration successful! Please login.';
          localStorage.setItem('token', res.token);
          this.redirectByRole(res.role);
          this.isLogin = true;
          this.form.reset();
        },
        error: (err) => {
          this.loading = false;
          this.errorMessage = 'Registration failed. Try again.';
        }
      });
    }
  }

  private redirectByRole(role: string) {
    if (role === 'COIFFEUR') {
      this.router.navigate(['/barber/dashboard']);
    } else {
      this.router.navigate(['/client/dashboard']);
    }
  }
  // Function bach t-checki wach password match
  passwordMatchValidator(g: FormGroup) {
    // ILA KNA F LOGIN: Mat-checki walo, rajje3 null (ya3ni Valid)
    if (this.isLogin) return null;

    const password = g.get('password')?.value;
    const confirmPassword = g.get('confirmPassword')?.value;

    if (!confirmPassword) return null;

    return password === confirmPassword ? null : { mismatch: true };
  }
}
