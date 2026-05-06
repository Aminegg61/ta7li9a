import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BarberSearchDto } from '../models/interfaces';

@Injectable({
  providedIn: 'root'
})
export class BarberService {
  private baseUrl = 'https://ta7li9a-backend.onrender.com/api/barber';

  constructor(private http: HttpClient) {}

  updateStatus(status: 'ACTIVE' | 'FULL' | 'OFFLINE'): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/status?status=${status}`, {});
  }
  getCurrentStatus(): Observable<string> {
   return this.http.get(`${this.baseUrl}/status`, { responseType: 'text' });
  }

  searchBarbers(query: string): Observable<BarberSearchDto[]> {
    return this.http.get<BarberSearchDto[]>(`http://localhost:/api/barbers/search?q=${query}`);
  }

  addBarber(barberId: number): Observable<string> {
    // Expect text response or format appropriately based on your backend
    return this.http.post('/api/barbers/add-barber/' + barberId, {}, { responseType: 'text' }) as Observable<string>;
  }

  getMyBarbers(): Observable<BarberSearchDto[]> {
    return this.http.get<BarberSearchDto[]>('https://ta7li9a-backend.onrender.com/api/barbers/my-barbers');
  }

  getMyFavorites(): Observable<BarberSearchDto[]> {
    return this.http.get<BarberSearchDto[]>('https://ta7li9a-backend.onrender.com/api/barbers/my-favorites');
  }

  removeBarber(barberId: number): Observable<void> {
    return this.http.delete<void>(`https://ta7li9a-backend.onrender.com/api/barbers/remove-barber/${barberId}`);
  }

  toggleFavorite(barberId: number): Observable<void> {
    return this.http.put<void>(`https://ta7li9a-backend.onrender.com/api/barbers/toggle-favorite/${barberId}`, {});
  }
  pauseWork(): Observable<any> {
    // T2ekked mn l-URL dyalk wach howa hada (masalan: baseUrl + '/barbers/pause')
    return this.http.post(`${this.baseUrl}/pause`, {});
  }

  resumeWork(): Observable<any> {
    return this.http.post(`${this.baseUrl}/resume`, {});
  }
}
