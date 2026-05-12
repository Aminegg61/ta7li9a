import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('token');
  const router = inject(Router); // 1. Injectina Router bach n-qdero n-diw l-klyan l-Login

  // 2. Khellit l-logs dyalek bach t-b9a t-chouf l-Live Test f l-Console dyalek
  console.log('🚨 Interceptor déclenché ! URL:', req.url);
  console.log('🔑 Token l9inah ? :', token ? 'Wyeh kayn' : 'La makaynch (null)');

  // 3. T-twjih dyal l-Request b l-Token
  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  // 4. L-Qaleb li zad l-developer (Catch Error) - Nadi l-Production!
  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Ila l-Backend (Spring Boot) rejje3 401 (Unauthorized) awla 403 (Forbidden)
      if (error.status === 401 || error.status === 403) {
        console.warn('⚠️ Token expired awla unauthorized. Logging out...');
        
        // K-n-ms7ou l-token l-miyyet
        localStorage.removeItem('token');
        
        // K-n-reddouh y-dir Login mn jdid
        router.navigate(['/auth']);
      }
      
      // K-n-rej3ou l-erreur bach y-t-aficha ila bghinah f chi blassa khra
      return throwError(() => error);
    })
  );
};
