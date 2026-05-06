import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // N-jbdou l-token mn local storage
  const token = localStorage.getItem('token'); 
  
  // Hadi ghadi t-goul lina f l-console wach l-interceptor khddam w wach l9a l-token
  console.log('🚨 Interceptor déclenché ! URL:', req.url);
  console.log('🔑 Token l9inah ? :', token ? 'Wyeh kayn' : 'La makaynch (null)');
  
  if (token) {
    const authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
    return next(authReq);
  }
  
  return next(req);
};
